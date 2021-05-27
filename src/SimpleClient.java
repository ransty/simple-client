package src;
/*
 * ISC License
 *
 * Copyright (c) 2017-2019, Hunter WB <hunterwb.com>, 2021 Keano Porcaro <keano@ransty.com>
 *
 * Permission to use, copy, modify, and/or distribute this software for any
 * purpose with or without fee is hereby granted, provided that the above
 * copyright notice and this permission notice appear in all copies.
 *
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 * WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR
 * ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 * WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 * ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF
 * OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */

import org.objectweb.asm.tree.ClassNode;

import javax.swing.*;
import java.applet.Applet;
import java.applet.AppletContext;
import java.applet.AppletStub;
import java.applet.AudioClip;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.InvalidParameterException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;

@SuppressWarnings("deprecation")
public final class SimpleClient implements AppletStub, AppletContext {

    public static void main(String[] args) throws Exception {
        System.setProperty("sun.awt.noerasebackground", "true"); // fixes resize flickering

        SimpleClient c = load();

        Applet applet = c.loadApplet();

        JarHelper helper = new JarHelper();
        helper.parseJar(new JarFile("runescape.jar"));

        //ClassNode clientClass = helper.getClasses().get("client.class");

        JFrame frame = new JFrame(c.title());
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.add(applet);
        frame.pack();
        frame.setMinimumSize(frame.getSize());
        frame.setPreferredSize(frame.getSize());
        frame.setLocationRelativeTo(null);

        frame.setVisible(true);

        applet.init();
        applet.start();
    }

    private final Map<String, String> properties;

    private final Map<String, String> parameters;

    private SimpleClient(
            Map<String, String> properties,
            Map<String, String> parameters
    ) {
        this.properties = properties;
        this.parameters = parameters;
    }

    public static SimpleClient load() throws IOException {
        Map<String, String> properties = new HashMap<>();
        Map<String, String> parameters = new HashMap<>();
        URL url = new URL("http://oldschool.runescape.com/jav_config.ws");
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream(), StandardCharsets.ISO_8859_1))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] split1 = line.split("=", 2);
                switch (split1[0]) {
                    case "param":
                        String[] split2 = split1[1].split("=", 2);
                        parameters.put(split2[0], split2[1]);
                        break;
                    case "msg":
                        // ignore
                        break;
                    default:
                        properties.put(split1[0], split1[1]);
                }
            }
        }
        return new SimpleClient(properties, parameters);
    }

    public Applet loadApplet() throws Exception {
        if (downloadClient(gamepackUrl().toString())) {
            System.out.println("Gamepack downloaded");
        }
        Applet applet = (Applet) classLoader(gamepackUrl()).loadClass(initialClass()).getDeclaredConstructor().newInstance();
        applet.setStub(this);
        applet.setMaximumSize(appletMaxSize());
        applet.setMinimumSize(appletMinSize());
        applet.setPreferredSize(applet.getMinimumSize());
        applet.setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
        return applet;
    }

    public boolean downloadClient(String link) {
        try {
            URL url = new URL(link);
            String referer = url.toExternalForm();

            URLConnection uc = url.openConnection();

            uc.addRequestProperty("Accept", "text/xml,application/xml,application/xhtml+xml,text/html;q=0.9,text/plain;q=0.8,image/png,*/*;q=0.5");
            uc.addRequestProperty("Accept-Charset", "ISO-8859-1,utf-8;q=0.7,*;q=0.7");
            uc.addRequestProperty("Accept-Encoding", "gzip,deflate");
            uc.addRequestProperty("Accept-Language", "en-gb,en;q=0.5");
            uc.addRequestProperty("Connection", "keep-alive");
            uc.addRequestProperty("Host", "www.runescape.com");
            uc.addRequestProperty("Keep-Alive", "300");
            if (referer != null) {
                uc.addRequestProperty("Referer", referer);
            }
            uc.addRequestProperty("User-Agent", "Mozilla/5.0 (Windows; U; Windows NT 5.1; en-GB; rv:1.8.0.6) Gecko/20060728 Firefox/1.5.0.6");

            BufferedInputStream in = new BufferedInputStream(uc.getInputStream());
            FileOutputStream fos = new FileOutputStream("runescape.jar");
            BufferedOutputStream bout = new BufferedOutputStream(fos, 1024);
            byte[] data = new byte[1024];
            int x;
            while ((x=in.read(data, 0, 1024))>=0) {
                bout.write(data, 0, x);
            }
            bout.close();
            in.close();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public String title() {
        return properties.get("title");
    }

    private Dimension appletMinSize() {
        return new Dimension(
                Integer.parseInt(properties.get("applet_minwidth")),
                Integer.parseInt(properties.get("applet_minheight"))
        );
    }

    private Dimension appletMaxSize() {
        return new Dimension(
                Integer.parseInt(properties.get("applet_maxwidth")),
                Integer.parseInt(properties.get("applet_maxheight"))
        );
    }

    private URL gamepackUrl() throws MalformedURLException {
        return new URL(properties.get("codebase") + properties.get("initial_jar"));
    }

    private String initialClass() {
        String fileName = properties.get("initial_class");
        return fileName.substring(0, fileName.length() - 6);
    }

    @Override public URL getCodeBase() {
        try {
            return new URL(properties.get("codebase"));
        } catch (MalformedURLException e) {
            throw new InvalidParameterException();
        }
    }

    @Override public URL getDocumentBase() {
        return getCodeBase();
    }

    @Override public boolean isActive() {
        return true;
    }

    @Override public String getParameter(String name) {
        return parameters.get(name);
    }

    @Override public void showDocument(URL url) {
        try {
            Desktop.getDesktop().browse(url.toURI());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override public void showDocument(URL url, String target) {
        showDocument(url);
    }

    @Override public AppletContext getAppletContext() {
        return this;
    }

    @Override public void appletResize(int width, int height) {}

    @Override public AudioClip getAudioClip(URL url) {
        throw new UnsupportedOperationException();
    }

    @Override public Image getImage(URL url) {
        throw new UnsupportedOperationException();
    }

    @Override public Applet getApplet(String name) {
        throw new UnsupportedOperationException();
    }

    @Override public Enumeration<Applet> getApplets() {
        throw new UnsupportedOperationException();
    }

    @Override public void showStatus(String status) {
        throw new UnsupportedOperationException();
    }

    @Override public void setStream(String key, InputStream stream) {
        throw new UnsupportedOperationException();
    }

    @Override public InputStream getStream(String key) {
        throw new UnsupportedOperationException();
    }

    @Override public Iterator<String> getStreamKeys() {
        throw new UnsupportedOperationException();
    }

    private static ClassLoader classLoader(URL jarUrl) throws IOException {
        Map<String, byte[]> files = new HashMap<>();
        try (JarInputStream jar = new JarInputStream(new BufferedInputStream(jarUrl.openStream()))) {
            JarEntry entry;
            while ((entry = jar.getNextJarEntry()) != null) {
                files.put('/' + entry.getName(), jar.readAllBytes());
            }
        }
        URL url = new URL("x-buffer", null, -1, "/", new URLStreamHandler() {
            @Override protected URLConnection openConnection(URL u) throws FileNotFoundException {
                byte[] data = files.get(u.getFile());
                if (data == null) throw new FileNotFoundException(u.getFile());
                return new URLConnection(u) {
                    @Override public void connect() {}
                    @Override public long getContentLengthLong() {
                        return data.length;
                    }
                    @Override public InputStream getInputStream() {
                        return new ByteArrayInputStream(data);
                    }
                };
            }
        });
        return new URLClassLoader(new URL[]{url});
    }
}