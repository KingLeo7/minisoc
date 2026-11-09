#!/usr/bin/env bash
cd "$(dirname "$0")"

if [ ! -f "out/minisoc/gui/SocGui.class" ]; then
    echo "[MiniSOC] Compiling Java classes..."
    mkdir -p out
    javac -d out src/minisoc/*.java src/minisoc/gui/*.java
    if [ $? -ne 0 ]; then
        echo "[ERROR] Compilation failed. Ensure JDK is installed."
        exit 1
    fi
fi

echo "[MiniSOC] Launching Security Operations Center GUI..."
java -cp out minisoc.gui.SocGui "$@"

