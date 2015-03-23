#!/bin/sh

# Build, setup, and install Scotch.

SCOTCH_LANG="scotch-lang"
DISTRIBUTIONS_DIR="build/distributions"

echo "\n[+] Scotch Setup [+]\n"

# Make sure there's a JDK installed
echo "-- Checking for an adequate JDK..."
which java > /dev/null
if [ $? -ne 0 ]; then
    echo "ERROR: Java is needed Scotch."
    echo "       Please install the appropriate JDK (1.8 Update 40 or higher) for your OS.\nExiting..."
    exit 1
fi

# Check for older version of Scotch and archive it
echo "-- Checking for previous Scotch installation..."
OLD_VERSION=$(ls ${PWD} | perl -ne'/([0-9].+[A-Z])/ && print $1')
if [ -z ${OLD_VERSION} ]; then
    echo "---- None found.\n"
else
    echo "---- Found older version.  ...Removing.\n"
    rm -rf ${PWD}/${SCOTCH_LANG}-${OLD_VERSION}
fi

echo  "-- Compiling Scotch..."
./gradlew distZip || exit 1

VERSION=$(ls ${DISTRIBUTIONS_DIR} | perl -ne'/([0-9].+[A-Z])/ && print $1')

echo "-- Unzipping Scotch..."
unzip -u ${PWD}/${DISTRIBUTIONS_DIR}/${SCOTCH_LANG}-${VERSION}.zip

echo "\n-- Add Scotch to your PATH and your ${SHELL} initialization file..."
echo "export PATH=\$PATH:${PWD}/${SCOTCH_LANG}-${VERSION}/bin"

echo "\nComplete!"
echo "Documentation can be found here: http://loganmcgrath.com/scotch-lang/index.html?page=home\n"

exit 0
