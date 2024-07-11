#!/bin/bash

# https://medium.com/simform-engineering/publishing-library-in-maven-central-part-2-515c5d54f566
# https://central.sonatype.org/register/central-portal/

if ! mkdir repo/; then
    echo "Exiting..."
    exit -1
fi

version="0.1.0"

mvn clean -DskipTests install -Dmaven.repo.local=./repo

cd "repo/io/github/nlscript/nlScript/$version/"

rm "nlScript-$version-tests.jar"
rm _remote.repositories

gpg -ab "nlScript-$version.pom"
gpg -ab "nlScript-$version.jar"
gpg -ab "nlScript-$version-sources.jar"
gpg -ab "nlScript-$version-javadoc.jar"

md5sum.exe "nlScript-$version.pom"         | cut -d' ' -f 1 > "nlScript-$version.pom.md5"
md5sum.exe "nlScript-$version.jar"         | cut -d' ' -f 1 > "nlScript-$version.jar.md5"
md5sum.exe "nlScript-$version-sources.jar" | cut -d' ' -f 1 > "nlScript-$version-sources.jar.md5"
md5sum.exe "nlScript-$version-javadoc.jar" | cut -d' ' -f 1 > "nlScript-$version-javadoc.jar.md5"

sha1sum.exe "nlScript-$version.pom"         | cut -d' ' -f 1 > "nlScript-$version.pom.sha1"
sha1sum.exe "nlScript-$version.jar"         | cut -d' ' -f 1 > "nlScript-$version.jar.sha1"
sha1sum.exe "nlScript-$version-sources.jar" | cut -d' ' -f 1 > "nlScript-$version-sources.jar.sha1"
sha1sum.exe "nlScript-$version-javadoc.jar" | cut -d' ' -f 1 > "nlScript-$version-javadoc.jar.sha1"

cd ../../../../../

zip "../maven-release-$version.zip" "io/github/nlscript/nlScript/$version/*"

echo "Don't forget to delete the temporary repository 'repo'"
