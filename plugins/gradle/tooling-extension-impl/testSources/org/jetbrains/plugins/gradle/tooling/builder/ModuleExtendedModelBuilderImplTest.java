// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.tooling.builder;

import com.intellij.openapi.util.io.FileUtil;
import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtil;
import org.gradle.tooling.model.DomainObjectSet;
import org.gradle.tooling.model.idea.IdeaModule;
import org.gradle.tooling.model.idea.IdeaSourceDirectory;
import org.gradle.util.GradleVersion;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.gradle.model.ExtIdeaContentRoot;
import org.jetbrains.plugins.gradle.model.ModuleExtendedModel;
import org.junit.Test;

import java.io.File;
import java.util.*;

import static org.junit.Assert.*;

/**
 * @author Vladislav.Soroka
 */
public class ModuleExtendedModelBuilderImplTest extends AbstractModelBuilderTest {

  private final boolean is50OrBetter;

  public ModuleExtendedModelBuilderImplTest(@NotNull String gradleVersion) {
    super(gradleVersion);
    is50OrBetter = GradleVersion.version(gradleVersion).getBaseVersion().compareTo(GradleVersion.version("5.0")) >= 0;
  }

  @Test
  public void testGradleSourcesSetsInterpretation() {
    final int modulesSize = 9;

    DomainObjectSet<? extends IdeaModule> ideaModules = allModels.getIdeaProject().getModules();

    List<ModuleExtendedModel> models =
      ContainerUtil.mapNotNull(ideaModules, (Function<IdeaModule, ModuleExtendedModel>)module -> {
        ModuleExtendedModel moduleExtendedModel = allModels.getModel(module, ModuleExtendedModel.class);

        assertNotNull(moduleExtendedModel);

        List<String> sourceDirectories = new ArrayList<>();
        List<String> resourceDirectories = new ArrayList<>();
        List<String> testResourceDirectories = new ArrayList<>();
        List<String> testDirectories = new ArrayList<>();
        List<String> excludeDirectories = new ArrayList<>();

        fillDirectories(moduleExtendedModel,
                        sourceDirectories, resourceDirectories,
                        testDirectories, testResourceDirectories,
                        excludeDirectories);

        if (module.getName().equals("defaultJavaModule") || module.getName().equals("moduleWithSourceSetDirBothAsResourceAndJava")) {
          assertEquals(ContainerUtil.newArrayList("src/main/java"), sourceDirectories);
          assertEquals(ContainerUtil.newArrayList("src/main/resources"), resourceDirectories);
          assertEquals(ContainerUtil.newArrayList("src/test/java"), testDirectories);
          assertEquals(ContainerUtil.newArrayList("src/test/resources"), testResourceDirectories);
          assertEquals(ContainerUtil.newArrayList(".gradle", "build"), excludeDirectories);
        }
        else if (module.getName().equals("moduleWithSourceSetDirBothAsResourceAndGroovy")) {
          assertEquals(ContainerUtil.newArrayList("src/main/groovy", "src/main/java"), sourceDirectories);
          assertEquals(ContainerUtil.newArrayList("src/main/resources"), resourceDirectories);
          assertEquals(ContainerUtil.newArrayList("src/test/groovy", "src/test/java"), testDirectories);
          assertEquals(ContainerUtil.newArrayList("src/test/resources"), testResourceDirectories);
          assertEquals(ContainerUtil.newArrayList(".gradle", "build"), excludeDirectories);
        }
        else if (module.getName().equals("moduleWithCustomSourceSet")) {
          assertEquals(ContainerUtil.newArrayList("src/custom/java", "src/main/java"), sourceDirectories);
          assertEquals(ContainerUtil.newArrayList("src/custom/resources", "src/main/resources"), resourceDirectories);
          assertEquals(ContainerUtil.newArrayList("src/test/java"), testDirectories);
          assertEquals(ContainerUtil.newArrayList("src/test/resources"), testResourceDirectories);
          assertEquals(ContainerUtil.newArrayList(".gradle", "build"), excludeDirectories);
        }
        else if (module.getName().equals("withIntegrationTests")) {
          assertEquals(ContainerUtil.newArrayList("src/main/java"), sourceDirectories);
          assertEquals(ContainerUtil.newArrayList("src/main/resources"), resourceDirectories);
          assertEquals(ContainerUtil.newArrayList(
            "src/integration-test/java", "src/integrationTest/java", "src/test/java"), testDirectories);
          assertEquals(ContainerUtil.newArrayList(
            "src/integration-test/resources",
            "src/integrationTest/resources",
            "src/test/resources"), testResourceDirectories);
          assertEquals(ContainerUtil.newArrayList(".gradle", "build"), excludeDirectories);
        }
        else if (module.getName().equals("testGradleSourcesSetsInterpretation")) {
          assertTrue(sourceDirectories.isEmpty());
          assertTrue(resourceDirectories.isEmpty());
          assertTrue(testDirectories.isEmpty());
          assertTrue(testResourceDirectories.isEmpty());
          assertEquals(ContainerUtil.newArrayList(".gradle", "build"), excludeDirectories);
        }
        else if (module.getName().equals("withIdeaPluginCustomization1")) {
          assertEquals(ContainerUtil.newArrayList("src/main/java"), sourceDirectories);
          assertEquals(ContainerUtil.newArrayList("src/main/resources"), resourceDirectories);
          assertEquals(ContainerUtil.newArrayList("src/intTest/java", "src/intTest/resources", "src/test/java"), testDirectories);
          assertEquals(ContainerUtil.newArrayList("src/test/resources"), testResourceDirectories);
          assertEquals(ContainerUtil.newArrayList(".gradle", "build", "some-extra-exclude-folder"), excludeDirectories);
        }
        else if (module.getName().equals("withIdeaPluginCustomization2")) {
          if (is50OrBetter) {
            assertEquals(ContainerUtil.newArrayList("src/main/java", "src/test/java"), sourceDirectories);
            assertEquals(ContainerUtil.newArrayList("src/main/resources"), resourceDirectories);
            assertTrue(testDirectories.isEmpty());
            assertEquals(ContainerUtil.newArrayList("src/test/resources"), testResourceDirectories);
            assertEquals(ContainerUtil.newArrayList(".gradle", "build"), excludeDirectories);
          } else {
            assertEquals(ContainerUtil.newArrayList("src/main/java", "src/test/java", "src/test/resources"), sourceDirectories);
            assertEquals(ContainerUtil.newArrayList("src/main/resources"), resourceDirectories);
            assertTrue(testDirectories.isEmpty());
            assertTrue(testResourceDirectories.isEmpty());
            assertEquals(ContainerUtil.newArrayList(".gradle", "build"), excludeDirectories);
          }
        }
        else if (module.getName().equals("withIdeaPluginCustomization3")) {
          assertEquals(ContainerUtil.newArrayList("src/main/java"), sourceDirectories);
          assertEquals(ContainerUtil.newArrayList("src/awesome-test/resources", "src/main/resources"), resourceDirectories);
          assertEquals(ContainerUtil.newArrayList("src/awesome-test/java", "src/test/java"), testDirectories);
          assertEquals(ContainerUtil.newArrayList("src/test/resources"), testResourceDirectories);
          assertEquals(ContainerUtil.newArrayList(".gradle", "build"), excludeDirectories);
        }
        else {
          fail();
        }

        return moduleExtendedModel;
      });

    assertEquals(modulesSize, models.size());
  }

  @Test
  public void testJavaExtendedModel() {
    final Map<String, ModuleExtendedModel> modulesMap = getModulesMap(ModuleExtendedModel.class);
    assertEquals(3, modulesMap.size());

    final ModuleExtendedModel rootModule = modulesMap.get(":");
    assertNotNull(rootModule);
    assertNull(rootModule.getJavaSourceCompatibility());

    final ModuleExtendedModel defaultJavaModule = modulesMap.get(":defaultJavaModule");
    assertNotNull(defaultJavaModule);
    assertNotNull(defaultJavaModule.getJavaSourceCompatibility());

    final ModuleExtendedModel javaModule = modulesMap.get(":javaModule");
    assertNotNull(javaModule);
    assertEquals("1.6", javaModule.getJavaSourceCompatibility());
  }

  private void fillDirectories(final ModuleExtendedModel model,
                               List<String> sourceDirectories, List<String> resourceDirectories,
                               List<String> testDirectories, List<String> resourceTestDirectories,
                               List<String> excludeDirectories) {
    for (ExtIdeaContentRoot contentRoot : model.getContentRoots()) {
      sourceDirectories.addAll(getAllPaths(contentRoot.getSourceDirectories(), model.getName()));
      resourceDirectories.addAll(getAllPaths(contentRoot.getResourceDirectories(), model.getName()));
      testDirectories.addAll(getAllPaths(contentRoot.getTestDirectories(), model.getName()));
      resourceTestDirectories.addAll(getAllPaths(contentRoot.getTestResourceDirectories(), model.getName()));
      excludeDirectories.addAll(getAllPaths(contentRoot.getExcludeDirectories(), model.getName()));
    }
  }

  private Collection<String> getAllPaths(Collection<? extends File> directories, final String moduleName) {
    List<String> list = ContainerUtil.map2List(directories, (Function<File, String>)sourceDirectory -> {
      String path =
        FileUtil.toCanonicalPath(FileUtil.getRelativePath(new File(testDir, moduleName), sourceDirectory));
      assertNotNull(path);
      return path.substring(path.indexOf("/") + 1);
    });
    Collections.sort(list);
    return list;
  }

  private Collection<String> getAllPaths(DomainObjectSet<? extends IdeaSourceDirectory> directories, final String moduleName) {
    List<String> list = ContainerUtil.map2List(directories, (Function<IdeaSourceDirectory, String>)sourceDirectory -> {
      String path =
        FileUtil.toCanonicalPath(FileUtil.getRelativePath(new File(testDir, moduleName), sourceDirectory.getDirectory()));
      assertNotNull(path);
      return path;
    });
    Collections.sort(list);
    return list;
  }

  @Override
  protected Set<Class> getModels() {
    return ContainerUtil.set(ModuleExtendedModel.class);
  }
}

