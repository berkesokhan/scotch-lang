#!/bin/sh

# Distill Scotch.

SCOTCH_LANG=scotch-lang
SCOTCH_WORKSPACE=scotch-workspace
SCOTCH_URL=git@github.com:lmcgrath/scotch-lang.git
DISTRIBUTIONS_DIR=build/distributions
GIT=$(which git) > /dev/null
if [ $? -ne 0 ]; then
    echo "ERROR: git is required to install Scotch.\nExiting..."
    exit 1
fi

echo "\n[+] Scotch Setup [+]\n"

# Check for Java
which java > /dev/null
if [ $? -ne 0 ]; then
    echo "ERROR: Java is needed Scotch."
    echo "       Please install Java (1.8 Update 40 or higher) for your OS.\nExiting..."
    exit 1
fi

# Make sure there's a JDK installed
echo "-- Checking for an adequate JDK..."
if [ $(uname) == "Darwin" ]; then
    ls /Library/Java/JavaVirtualMachines | grep -i jdk | grep -E "1.8.\d+" > /dev/null
    if [ $? -ne 0 ]; then
        echo "ERROR: No suitable JDK found.\nExiting..."
        exit 1
    fi
fi

# Create the Scotch workspace
echo "-- Creating Scotch workspace..."
mkdir -p ${SCOTCH_WORKSPACE}/${SCOTCH_LANG}

cd ${SCOTCH_WORKSPACE}

# Clone Scotch to repo directory
echo "-- Cloning Scotch to ${SCOTCH_WORKSPACE}/${SCOTCH_LANG}...\n"
${GIT} clone ${SCOTCH_URL} ${SCOTCH_LANG} || exit 1


# Check for older version of Scotch and archive it
echo "\n-- Checking for previous Scotch installation..."
OLD_VERSION=$(ls | perl -ne'/([0-9].+[A-Z])/ && print $1')
if [ -z ${OLD_VERSION} ]; then
    echo "---- None found.\n"
else
    echo "---- Found older version.  ...Removing.\n"
    rm -rf ${SCOTCH_LANG}-${OLD_VERSION}
fi

# Compile Scotch
echo  "-- Compiling Scotch..."
cd ${SCOTCH_LANG}
./gradlew distZip || exit 1

VERSION=$(ls ${DISTRIBUTIONS_DIR} | perl -ne'/([0-9].+[A-Z])/ && print $1')

# Unzip Scotch into the Scotch workspace
echo "-- Unzipping Scotch..."
cd ..
unzip -u ${SCOTCH_LANG}/${DISTRIBUTIONS_DIR}/${SCOTCH_LANG}-${VERSION}.zip || exit 1

echo "\n-- Add Scotch to your PATH and your ${SHELL} initialization file..."
echo "export PATH=\$PATH:${PWD}/${SCOTCH_LANG}-${VERSION}/bin"

# Remove git repository
echo "\n-- Removing ${SCOTCH_WORKSPACE}/${SCOTCH_LANG}"
rm -rf ${SCOTCH_LANG}

echo "\nComplete!"
echo "Documentation can be found here: http://loganmcgrath.com/scotch-lang/index.html?page=home\n"

exit 0
