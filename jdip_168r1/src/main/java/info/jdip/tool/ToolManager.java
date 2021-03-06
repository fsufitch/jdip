//
//  @(#)ToolManager.java	1.00	9/2002
//
//  Copyright 2002 Zachary DelProposto. All rights reserved.
//  Use is subject to license terms.
//
//
//  This program is free software; you can redistribute it and/or modify
//  it under the terms of the GNU General Public License as published by
//  the Free Software Foundation; either version 2 of the License, or
//  (at your option) any later version.
//
//  This program is distributed in the hope that it will be useful,
//  but WITHOUT ANY WARRANTY; without even the implied warranty of
//  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//  GNU General Public License for more details.
//
//  You should have received a copy of the GNU General Public License
//  along with this program; if not, write to the Free Software
//  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
//  Or from http://www.gnu.org/
//
package info.jdip.tool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

/**
 * Manages Tool plugins.
 */
public class ToolManager {
    private static final Logger logger = LoggerFactory.getLogger(ToolManager.class);
    // constants
    private static final String TOOL_EXT_JAR = "Tool.jar";

    // class variables
    private static ToolManager tm = null;

    // instance variables
    private URLClassLoader toolClassLoader = null;
    private Tool[] tools = new Tool[0];

    /**
     * (Singleton) Constructor
     */
    private ToolManager() {
    }// ToolManager()

    /**
     * Initialize the ToolManager. No other methods are guaranteed to work
     * until the ToolManager singleton has been initialized.
     */
    public static synchronized void init(File[] searchPaths) {
        tm = new ToolManager();

        // search for Tools
        final File[] foundToolFiles = tm.searchForFiles(searchPaths);    // no null entries
        URL[] foundToolURLs = new URL[foundToolFiles.length];            // entries will be null if invalid
        String[] mainClassNames = new String[foundToolURLs.length];        // entries will be null if invalid

        for (int i = 0; i < foundToolFiles.length; i++) {
            // defaults
            foundToolURLs[i] = null;
            mainClassNames[i] = null;

            // attempt file-to-URL conversion
            try {
                foundToolURLs[i] = foundToolFiles[i].toURI().toURL();
            } catch (java.net.MalformedURLException e) {
                logger.error("Could not convert to URL: {}", foundToolFiles[i], e);
            }

            // do not attempt if URL is null.
            if (foundToolURLs[i] != null) {
                try {
                    JarFile jarFile = new JarFile(foundToolFiles[i], true, JarFile.OPEN_READ);
                    Manifest manifest = jarFile.getManifest();
                    Attributes attr = manifest.getMainAttributes();
                    mainClassNames[i] = attr.getValue(Attributes.Name.MAIN_CLASS);
                    jarFile.close();
                } catch (IOException e) {
                    mainClassNames[i] = null;
                    logger.error("Could not find main-class attribute in manifest for tool: {}",
                            foundToolFiles[i], e.getMessage());
                }
            }
        }

        tm.toolClassLoader = new URLClassLoader(foundToolURLs);


        // for each Tool, attempt to load its main class (same as the file name, without the
        // file extension) and add it to the Tool array
        ArrayList<Tool> list = new ArrayList<>();
        for (int i = 0; i < foundToolURLs.length; i++) {
            if (mainClassNames[i] != null && foundToolURLs[i] != null) {
                try {
                    Tool tool = (Tool) tm.toolClassLoader.loadClass(mainClassNames[i]).getDeclaredConstructor().newInstance();
                    list.add(tool);
                } catch (Throwable e) {
                    logger.error("There was a problem loading Tool: {}", e);
                }
            }
        }

        tm.tools = list.toArray(new Tool[list.size()]);
    }// init()

    /**
     * Returns all Tool objects loaded. Never returns null.
     */
    public static synchronized Tool[] getTools() {
        checkTM();
        return tm.tools;
    }// getTools()

    /**
     * Ensures that we have initialized the ToolManager
     */
    private static void checkTM() {
        if (tm == null) {
            throw new IllegalArgumentException("not initialized");
        }
    }// checkTM()

    /**
     * Searches the paths for plugins, and returns the URL to each.
     */
    private File[] searchForFiles(File[] searchPaths) {
        List<File> fileList = new ArrayList<>();

        for (File searchPath : searchPaths) {
            logger.info("Searching for tools on: {}", searchPath);
            File[] list = searchPath.listFiles();
            if (list != null) {
                for (File file : list) {
                    if (file.isFile()) {
                        String fileName = file.getPath();
                        if (fileName.endsWith(TOOL_EXT_JAR)) {
                            logger.info("Found tool: {}", file);
                            fileList.add(file);
                        }
                    }
                }
            }
        }

        return fileList.toArray(new File[fileList.size()]);
    }// searchForFiles()
}// class ToolManager






