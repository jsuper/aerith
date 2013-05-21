/*
 *                 Sun Public License Notice
 *
 * The contents of this file are subject to the Sun Public License
 * Version 1.0 (the "License"). You may not use this file except in
 * compliance with the License. A copy of the License is available at
 * http://www.sun.com/
 *
 * The Original Code is NetBeans. The Initial Developer of the Original
 * Code is Sun Microsystems, Inc. Portions Copyright 1997-2005 Sun
 * Microsystems, Inc. All Rights Reserved.
 */

package com.sun.javaone.aerith.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.ResourceBundle;

/** Convenience class permitting easy loading of localized resources of various sorts.
* Extends the functionality of the default Java resource support, and interacts
* better with class loaders in a multiple-loader system.
* <p>Example usage:
* <p><code><pre>
* package com.mycom;
* public class Foo {
*   // Search for tag Foo_theMessage in /com/mycom/Bundle.properties:
*   private static String theMessage = {@link NbBundle#getMessage(Class, String) NbBundle.getMessage} (Foo.class, "Foo_theMessage");
*   // Might also look in /com/mycom/Bundle_de.properties, etc.
* }
* </pre></code>
*/
public class Bundles {
    /**
     * Cache of resource bundles.
     */
    private static final Map<String,ResourceBundle> bundleCache = new HashMap<String,ResourceBundle>();

    /**
     * Do not call.
     */
    private Bundles() {
    }

    // ---- LOADING RESOURCE BUNDLES ----

    /**
    * Get a resource bundle with the default class loader and locale/branding.
    * <strong>Caution:</strong> {@link #getBundle(Class)} is generally
    * safer when used from a module as this method relies on the module's
    * classloader to currently be part of the system classloader. The
    * IDE does add enabled modules to this classloader, however calls to
    * this variant of the method made in <a href="@org-openide-modules@/org/openide/modules/ModuleInstall.html#validate()>ModuleInstall.validate</a>,
    * or made soon after a module is uninstalled (due to background threads)
    * could fail unexpectedly.
    * @param baseName bundle basename
    * @return the resource bundle
    * @exception MissingResourceException if the bundle does not exist
    */
    public static final ResourceBundle getBundle(String baseName)
    throws MissingResourceException {
        return getBundle(baseName, Locale.getDefault(),Bundles.class.getClassLoader());
    }

    /** Get a resource bundle in the same package as the provided class,
    * with the default locale/branding and the class' own classloader.
    * This is the usual style of invocation.
    *
    * @param clazz the class to take the package name from
    * @return the resource bundle
    * @exception MissingResourceException if the bundle does not exist
    */
    public static ResourceBundle getBundle(Class clazz)
    throws MissingResourceException {
        String name = findName(clazz);

        return getBundle(name, Locale.getDefault(), clazz.getClassLoader());
    }

    /** Finds package name for given class */
    private static String findName(Class clazz) {
        String pref = clazz.getName();
        int last = pref.lastIndexOf('.');

        if (last >= 0) {
            pref = pref.substring(0, last + 1);

            return pref + "Bundle"; // NOI18N
        } else {
            // base package, search for bundle
            return "Bundle"; // NOI18N
        }
    }

    /**
    * Get a resource bundle with the default class loader and branding.
    * @param baseName bundle basename
    * @param locale the locale to use (but still uses {@link #getBranding default branding})
    * @return the resource bundle
    * @exception MissingResourceException if the bundle does not exist
    */
    public static final ResourceBundle getBundle(String baseName, Locale locale)
    throws MissingResourceException {
        return getBundle(baseName, locale, Bundles.class.getClassLoader());
    }

    /** Get a resource bundle the hard way.
    * @param baseName bundle basename
    * @param locale the locale to use (but still uses {@link #getBranding default branding})
    * @param loader the class loader to use
    * @return the resource bundle
    * @exception MissingResourceException if the bundle does not exist
    */
    public static final ResourceBundle getBundle(String baseName, Locale locale, ClassLoader loader)
    throws MissingResourceException {
        // Could more simply use ResourceBundle.getBundle (plus some special logic
        // with MergedBundle to handle branding) instead of manually finding bundles.
        // However this code is faster and has some other desirable properties.

        //A minor optimization to cut down on StringBuffer allocations - OptimizeIt
        //showed the commented out code below was a major source of them.  This
        //just does the same thing with a char array - Tim
        String localeStr = locale.toString();
        char[] k = new char[baseName.length() + 3 + localeStr.length()];
        baseName.getChars(0, baseName.length(), k, 0);
        k[baseName.length()] = '/'; //NOI18N

        int pos = baseName.length() + 1;

        k[pos] = '-'; //NOI18N
        pos++;

        k[pos] = '/'; //NOI18N
        pos++;
        
        localeStr.getChars(0, localeStr.length(), k, pos);

        String key = new String(k);

        /*
        String key = name + '/' + "-" + '/' + locale; // NOI18N
         */
        ResourceBundle b = null;
        synchronized (bundleCache) {
            Object o = bundleCache.get(key);
            b = (o != null) ? (ResourceBundle)o : null;

            if (b != null) {
                return b;
            } else {
                b = loadBundle(baseName, locale, loader);

                if (b != null) {
                    bundleCache.put(key, b);
                    return b;
                } else {
                    MissingResourceException e = new MissingResourceException("No such bundle " + baseName, baseName, null); // NOI18N
                    throw e;
                }
            }
        }
    }

    /**
     * Load a resource bundle (without caching).
     * @param name the base name of the bundle, e.g. <samp>org.netbeans.modules.foo.Bundle</samp>
     * @param locale the locale to use
     * @param loader a class loader to search in
     * @return a resource bundle (locale- and branding-merged), or null if not found
     */
    @SuppressWarnings("unchecked")
    private static ResourceBundle loadBundle(String name, Locale locale, ClassLoader loader) {
        String sname = name.replace('.', '/');
        Iterator it = new LocaleIterator(locale);
        LinkedList l = new LinkedList();

        while (it.hasNext()) {
            l.addFirst(it.next());
        }

        it = l.iterator();

        Properties p = new Properties();
        boolean first = true;

        while (it.hasNext()) {
            String res = sname + (String) it.next() + ".properties";

            // #49961: don't use getResourceAsStream; catch all errors opening it
            URL u = loader.getResource(res);

            if (u != null) {
                //System.err.println("Loading " + res);
                try {
                    // #51667: but in case we are in USE_DEBUG_LOADER mode, use gRAS (since getResource is not overridden)
                    InputStream is = u.openStream();

                    try {
                        p.load(is);
                    } finally {
                        is.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    return null;
                }
            } else if (first) {
                // No base *.properties. Try *.class.
                // Note that you may not mix *.properties w/ *.class this way.
                return loadBundleClass(name, sname, locale, l, loader);
            }

            first = false;
        }

        return new PBundle(p, locale);
    }

    /**
     * Load a class-based resource bundle.
     * @param name the base name of the bundle, e.g. <samp>org.netbeans.modules.foo.Bundle</samp>
     * @param sname the name with slashes, e.g. <samp>org/netbeans/modules/foo/Bundle</samp>
     * @param locale the locale to use
     * @param suffixes a list of suffixes to apply to the bundle name, in <em>increasing</em> order of specificity
     * @param loader a class loader to search in
     * @return a resource bundle (merged according to the suffixes), or null if not found
     */
    private static ResourceBundle loadBundleClass(
        String name, String sname, Locale locale, List suffixes, ClassLoader l
    ) {
        if (l.getResource(sname + ".class") == null) { // NOI18N

            // No chance - no base bundle. Don't waste time catching CNFE.
            return null;
        }

        ResourceBundle master = null;
        Iterator it = suffixes.iterator();

        while (it.hasNext()) {
            try {
                Class c = Class.forName(name + (String) it.next(), true, l);
                ResourceBundle b = (ResourceBundle) c.newInstance();

                if (master == null) {
                    master = b;
                } else {
                    master = new MergedBundle(locale, b, master);
                }
            } catch (ClassNotFoundException cnfe) {
                // fine - ignore
            } catch (Exception e) {
                e.printStackTrace();
            } catch (LinkageError e) {
                e.printStackTrace();
            }
        }

        return master;
    }

    //
    // Helper methods to simplify localization of messages
    //

    /**
     * Finds a localized and/or branded string in a bundle.
    * @param clazz the class to use to locate the bundle
    * @param resName name of the resource to look for
    * @return the string associated with the resource
    * @throws MissingResourceException if either the bundle or the string cannot be found
    */
    public static String getMessage(Class clazz, String resName)
    throws MissingResourceException {
        return getBundle(clazz).getString(resName);
    }

   /**
    * Finds a localized and/or branded string in a bundle and formats the message
    * by passing requested parameters.
    *
    * @param clazz the class to use to locate the bundle
    * @param resName name of the resource to look for
    * @param params array of parameters to use for formatting the message
    * @return the string associated with the resource
    * @throws MissingResourceException if either the bundle or the string cannot be found
    * @see java.text.MessageFormat#format(String,Object[])
    */
    public static String getMessage(Class clazz, String resName, Object... params)
    throws MissingResourceException {
        return java.text.MessageFormat.format(getMessage(clazz, resName), params);
    }

    /**
     * A resource bundle based on <samp>.properties</samp> files (or any map).
     */
    private static final class PBundle extends ResourceBundle {
        private final Map m; // Map<String,String>
        private final Locale locale;

        /**
         * Create a new bundle based on a map.
         * @param m a map from resources keys to values (typically both strings)
         * @param locale the locale it represents <em>(informational)</em>
         */
        public PBundle(Map m, Locale locale) {
            this.m = m;
            this.locale = locale;
        }

        @SuppressWarnings("unchecked")
        public Enumeration getKeys() {
            return Collections.enumeration(m.keySet());
        }

        protected Object handleGetObject(String key) {
            return m.get(key);
        }

        public Locale getLocale() {
            return locale;
        }
    }

    /** Special resource bundle which delegates to two others.
     * Ideally could just set the parent on the first, but this is protected, so...
     */
    private static class MergedBundle extends ResourceBundle {
        private Locale loc;
        private ResourceBundle sub1;
        private ResourceBundle sub2;

        /**
         * Create a new bundle delegating to two others.
         * @param loc the locale it represents <em>(informational)</em>
         * @param sub1 one delegate (taking precedence over the other in case of overlap)
         * @param sub2 the other (weaker) delegate
         */
        public MergedBundle(Locale loc, ResourceBundle sub1, ResourceBundle sub2) {
            this.loc = loc;
            this.sub1 = sub1;
            this.sub2 = sub2;
        }

        public Locale getLocale() {
            return loc;
        }

        protected Object handleGetObject(String key) throws MissingResourceException {
            try {
                return sub1.getObject(key);
            } catch (MissingResourceException mre) {
                // Ignore exception, and...
                return sub2.getObject(key);
            }
        }

        //TODO do I have to implement this?
        public Enumeration<String> getKeys() {
            return new Enumeration<String>() {
                public boolean hasMoreElements() {
                    throw new AssertionError("Have to implement");
                }
                public String nextElement() {
                    throw new AssertionError("Have to implement");
                }
            };
        }
    }

    /** This class (enumeration) gives all localized sufixes using nextElement
    * method. It goes through given Locale and continues through Locale.getDefault()
    * Example 1:
    *   Locale.getDefault().toString() -> "_en_US"
    *   you call new LocaleIterator(new Locale("cs", "CZ"));
    *  ==> You will gets: "_cs_CZ", "_cs", "", "_en_US", "_en"
    *
    * Example 2:
    *   Locale.getDefault().toString() -> "_cs_CZ"
    *   you call new LocaleIterator(new Locale("cs", "CZ"));
    *  ==> You will gets: "_cs_CZ", "_cs", ""
    *
    * If there is a branding token in effect, you will get it too as an extra
    * prefix, taking precedence, e.g. for the token "f4jce":
    *
    * "_f4jce_cs_CZ", "_f4jce_cs", "_f4jce", "_f4jce_en_US", "_f4jce_en", "_cs_CZ", "_cs", "", "_en_US", "_en"
    *
    * Branding tokens with underscores are broken apart naturally: so e.g.
    * branding "f4j_ce" looks first for "f4j_ce" branding, then "f4j" branding, then none.
    */
    private static class LocaleIterator extends Object implements Iterator {
        /** this flag means, if default locale is in progress */
        private boolean defaultInProgress = false;

        /** this flag means, if empty sufix was exported yet */
        private boolean empty = false;

        /** current locale, and initial locale */
        private Locale locale;

        /** current locale, and initial locale */
        private Locale initLocale;

        /** current sufix which will be returned in next calling nextElement */
        private String current;

        /** the branding string in use */
        private String branding;

        /** Creates new LocaleIterator for given locale.
        * @param locale given Locale
        */
        public LocaleIterator(Locale locale) {
            this.locale = this.initLocale = locale;

            if (locale.equals(Locale.getDefault())) {
                defaultInProgress = true;
            }

            current = '_' + locale.toString();

            branding = null;

            //System.err.println("Constructed: " + this);
        }

        /** @return next sufix.
        * @exception NoSuchElementException if there is no more locale sufix.
        */
        public Object next() throws NoSuchElementException {
            if (current == null) {
                throw new NoSuchElementException();
            }

            final String ret;

            if (branding == null) {
                ret = current;
            } else {
                ret = branding + current;
            }

            int lastUnderbar = current.lastIndexOf('_');

            if (lastUnderbar == 0) {
                if (empty) {
                    reset();
                } else {
                    current = ""; // NOI18N
                    empty = true;
                }
            } else {
                if (lastUnderbar == -1) {
                    if (defaultInProgress) {
                        reset();
                    } else {
                        // [PENDING] stuff with trying the default locale
                        // after the real one does not actually seem to work...
                        locale = Locale.getDefault();
                        current = '_' + locale.toString();
                        defaultInProgress = true;
                    }
                } else {
                    current = current.substring(0, lastUnderbar);
                }
            }

            //System.err.println("Returning: `" + ret + "' from: " + this);
            return ret;
        }

        /** Finish a series.
         * If there was a branding prefix, restart without that prefix
         * (or with a shorter prefix); else finish.
         */
        private void reset() {
            if (branding != null) {
                current = '_' + initLocale.toString();

                int idx = branding.lastIndexOf('_');

                if (idx == 0) {
                    branding = null;
                } else {
                    branding = branding.substring(0, idx);
                }

                empty = false;
            } else {
                current = null;
            }
        }

        /** Tests if there is any sufix.*/
        public boolean hasNext() {
            return (current != null);
        }

        public void remove() throws UnsupportedOperationException {
            throw new UnsupportedOperationException();
        }
    }
     // end of LocaleIterator
}
