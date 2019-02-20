/*
 * Copyright 2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.java

import org.gradle.integtests.fixtures.AbstractHttpDependencyResolutionTest
import org.gradle.integtests.fixtures.resolve.ResolveTestFixture
import org.gradle.test.fixtures.server.http.MavenHttpModule
import spock.lang.Unroll

class JavaLibraryPublishedTargetPlatformIntegrationTest extends AbstractHttpDependencyResolutionTest {
    ResolveTestFixture resolve
    MavenHttpModule module

    def setup() {
        settingsFile << """
            rootProject.name = 'test'
        """
        buildFile << """
            apply plugin: 'java-library'
            
            repositories {
                maven { url '${mavenHttpRepo.uri}' }
            }
            
            dependencies {
                api 'org:producer:1.0'
            }
        """

        resolve = new ResolveTestFixture(buildFile, 'compileClasspath')
        resolve.prepare()

        module = mavenHttpRepo.module('org', 'producer', '1.0')
                .withModuleMetadata()
                .withGradleMetadataRedirection()
                .adhocVariants()
                .variant("apiElementsJdk6", [
                'org.gradle.dependency.bundling': 'external',
                'org.gradle.java.min.platform': '6',
                'org.gradle.usage': 'java-api-jars'], { artifact('producer-1.0-jdk6.jar') })
                .variant("apiElementsJdk7", [
                'org.gradle.dependency.bundling': 'external',
                'org.gradle.java.min.platform': '7',
                'org.gradle.usage': 'java-api-jars'], { artifact('producer-1.0-jdk7.jar') })
                .variant("apiElementsJdk9", [
                'org.gradle.dependency.bundling': 'external',
                'org.gradle.java.min.platform': '9',
                'org.gradle.usage': 'java-api-jars'], { artifact('producer-1.0-jdk9.jar') })
                .variant("runtimeElementsJdk6", [
                'org.gradle.dependency.bundling': 'external',
                'org.gradle.java.min.platform': '6',
                'org.gradle.usage': 'java-runtime-jars'], { artifact('producer-1.0-jdk6.jar') })
                .variant("runtimeElementsJdk7", [
                'org.gradle.dependency.bundling': 'external',
                'org.gradle.java.min.platform': '7',
                'org.gradle.usage': 'java-runtime-jars'], { artifact('producer-1.0-jdk7.jar') })
                .variant("runtimeElementsJdk9", [
                'org.gradle.dependency.bundling': 'external',
                'org.gradle.java.min.platform': '9',
                'org.gradle.usage': 'java-runtime-jars'], { artifact('producer-1.0-jdk9.jar') })
                .publish()

    }

    def "can fail resolution if producer doesn't have appropriate target version"() {
        buildFile << """
            configurations.compileClasspath.attributes.attribute(TargetJavaPlatform.MINIMAL_TARGET_PLATFORM_ATTRIBUTE, 5)
        """

        when:
        module.pom.expectGet()
        module.moduleMetadata.expectGet()

        fails ':checkDeps'

        then:
        failure.assertHasCause('''Unable to find a matching variant of org:producer:1.0:
  - Variant 'apiElementsJdk6' capability org:producer:1.0:
      - Found org.gradle.dependency.bundling 'external' but wasn't required.
      - Required org.gradle.java.min.platform '5' and found incompatible value '6'.
      - Found org.gradle.status 'release' but wasn't required.
      - Required org.gradle.usage 'java-api' and found compatible value 'java-api-jars'.
  - Variant 'apiElementsJdk7' capability org:producer:1.0:
      - Found org.gradle.dependency.bundling 'external' but wasn't required.
      - Required org.gradle.java.min.platform '5' and found incompatible value '7'.
      - Found org.gradle.status 'release' but wasn't required.
      - Required org.gradle.usage 'java-api' and found compatible value 'java-api-jars'.
  - Variant 'apiElementsJdk9' capability org:producer:1.0:
      - Found org.gradle.dependency.bundling 'external' but wasn't required.
      - Required org.gradle.java.min.platform '5' and found incompatible value '9'.
      - Found org.gradle.status 'release' but wasn't required.
      - Required org.gradle.usage 'java-api' and found compatible value 'java-api-jars'.''')
    }

    @Unroll
    def "can select the most appropriate producer variant (#expected) based on target compatibility (#requested)"() {
        buildFile << """
            configurations.compileClasspath.attributes.attribute(TargetJavaPlatform.MINIMAL_TARGET_PLATFORM_ATTRIBUTE, $requested)
        """

        when:
        module.pom.expectGet()
        module.moduleMetadata.expectGet()
        module.getArtifact(classifier: "jdk${selected}").expectGet()

        run ':checkDeps'

        then:
        resolve.expectGraph {
            root(":", ":test:") {
                module('org:producer:1.0') {
                    variant(expected, [
                            'org.gradle.dependency.bundling': 'external',
                            'org.gradle.java.min.platform': selected,
                            'org.gradle.usage': 'java-api-jars',
                            'org.gradle.status': 'release'
                    ])
                    artifact(classifier: "jdk${selected}")
                }
            }
        }

        where:
        requested | selected
        6         | 6
        7         | 7
        8         | 7
        9         | 9
        10        | 9

        expected = "apiElementsJdk$selected"
    }
}