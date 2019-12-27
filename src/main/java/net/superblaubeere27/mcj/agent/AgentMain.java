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

import net.superblaubeere27.asmdelta.ASMDeltaPatch;
import net.superblaubeere27.asmdelta.difference.AbstractDifference;

import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.Instrumentation;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

public class AgentMain {

    public static void premain(String agentArgs, Instrumentation inst) {

    }

    public static void agentmain(String agentArgs, Instrumentation inst) {
        ASMDeltaPatch patch;

        try (InputStream inputStream = AgentMain.class.getResourceAsStream("/patch.asmdelta")) {
            patch = ASMDeltaPatch.read(inputStream);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read patch");
        }

        HashMap<String, HashSet<AbstractDifference>> patchMap = new HashMap<>();

        // Grouping patches by class names
        for (AbstractDifference abstractDifference : patch.getDifferenceList()) {
            patchMap.computeIfAbsent(abstractDifference.getClassName().replace('/', '.'), e -> new HashSet<>()).add(abstractDifference);
        }

        System.out.println("Loaded " + patch.getDifferenceList().size() + " patches for " + patchMap.size() + " classes");
        System.out.println("Applying patch " + patch.getPatchName());


        try {
            inst.addTransformer(new ASMDeltaTransformer(patchMap), true);

            // Only retransform classes which are affected by the patches
            Arrays.stream(inst.getAllLoadedClasses())
                    .filter(c -> patchMap.containsKey(c.getName()))
                    .forEach(clazz -> {
                        try {
                            inst.retransformClasses(clazz);
                        } catch (Throwable e) {
                            System.err.println("Failed to retransform class " + clazz.getName());
                            e.printStackTrace();
                        }
                    });


        } catch (Throwable e) {
            System.err.println("Failed to initialize agent");
            e.printStackTrace();
        }
    }

}
