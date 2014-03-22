/*
 * Copyright 2013 the original author or authors.
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



package org.gradle.api.internal.tasks.compile.incremental.graph

import org.gradle.api.internal.tasks.compile.incremental.ClassDependents
import org.gradle.test.fixtures.file.TestNameTestDirectoryProvider
import org.junit.Rule
import spock.lang.Specification

class ClassDependencyInfoSerializerTest extends Specification {

    @Rule TestNameTestDirectoryProvider temp = new TestNameTestDirectoryProvider()

    def "stores dependency info"() {
        def s = new ClassDependencyInfoSerializer(temp.file("foo.bin"))

        when:
        s.writeInfo(new ClassDependencyInfo(["foo.Foo": ClassDependents.dependentsSet(["bar.Bar"])]))
        def info = s.provideInfo()

        then:
        info.getRelevantDependents("foo.Foo").dependentClasses == ["bar.Bar"] as Set
    }
}
