/*
 * MIT License
 *
 * Copyright (c) 2019 superblaubeere27
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package net.superblaubeere27.mcj.agent;

import net.superblaubeere27.asmdelta.difference.AbstractDifference;
import net.superblaubeere27.asmdelta.difference.VerificationException;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;
import java.util.HashMap;
import java.util.HashSet;

public class ASMDeltaTransformer implements ClassFileTransformer {
    private HashMap<String, HashSet<AbstractDifference>> patches;

    public ASMDeltaTransformer(HashMap<String, HashSet<AbstractDifference>> patches) {
        this.patches = patches;
    }

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) {
        if (!patches.containsKey(className))
            return null;

        ClassReader classReader = new ClassReader(classfileBuffer);
        ClassNode classNode = new ClassNode();

        // The debug information is removed since it isn't needed and causes bugs
        classReader.accept(classNode, ClassReader.SKIP_DEBUG);


        HashMap<String, ClassNode> map = new HashMap<>();
        // The patch has to be applied to this one class only since they have already been grouped in AgentMain
        map.put(classNode.name, classNode);

        for (AbstractDifference abstractDifference : patches.get(className)) {
            try {
                abstractDifference.apply(map);
            } catch (VerificationException e) {
                throw new IllegalStateException("Failed to apply patch", e);
            }
        }

        // Recalculate frames and maximums. All classes are available at runtime
        // so it makes the agent a lot safer from producing illegal classes
        ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);

        classNode.accept(classWriter);


        return classWriter.toByteArray();
    }
}
