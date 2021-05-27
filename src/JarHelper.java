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

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

public class JarHelper {

    private Manifest manifest;

    private Map<String, ClassNode> classes = new HashMap<>();

    public Map<String, ClassNode> getClasses() {
        return classes;
    }

    public void parseJar(JarFile file) {
        Objects.requireNonNull(file, "The jar file must not be null!");
        try {
            Enumeration<JarEntry> entries = file.entries();
            manifest = file.getManifest();
            do {
                JarEntry entry = entries.nextElement();

                if (!entry.getName().contains(".class")) continue;

                ClassReader classReader = new ClassReader(file.getInputStream(entry));
                ClassNode classNode = new ClassNode();
                classReader.accept(classNode, ClassReader.SKIP_DEBUG);
                classes.put(entry.getName(), classNode);

            } while (entries.hasMoreElements());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void saveJar() {
        try (JarOutputStream jos = new JarOutputStream(new FileOutputStream("newjar.jar"), manifest)) {

            Collection<ClassNode> classNodes = classes.values();

            List<String> names = new ArrayList<>();

            for (ClassNode node : classNodes) {
                if (names.contains(node.name)) continue;

                JarEntry newEntry = new JarEntry(node.name + ".class");
                jos.putNextEntry(newEntry);

                ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
                node.accept(writer);
                jos.write(writer.toByteArray());

                names.add(node.name);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
