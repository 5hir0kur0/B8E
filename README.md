# B8E: Basic 8051 Emulator (and Assembler)

B8E (pronounced /ˈbeɪ.ti/) is a simple emulator / assembler for the [Intel 8051](https://en.wikipedia.org/wiki/Intel_MCS-51) micro controller architecture.
We (@Noxgrim and @5hir0kur0) made this as a project for our IT class in high school.

# Features
- GUI with File Tree, Tabs, and Clickable Error/Warning Messages
- Assembler with Preprocessor Directives
- Customizable Syntax Highlighting
- Emulator/Debugger
- Storing/Resuming Emulator State
- Intel-HEX Output of the Assembled Binary

Take a look at the Wiki for more documentation.

# Screenshots
Main Window:
![Main Window Screenshot](https://raw.githubusercontent.com/5hir0kur0/B8E/screenshots/main_window.png)
Emulator Window:
![Emulator Window Screenshot](https://raw.githubusercontent.com/5hir0kur0/B8E/screenshots/emulator_window.png)

# Building
Java Version: 1.8

Execute `build.sh` from the project root:
```sh
cd B8E
./build.sh
```
