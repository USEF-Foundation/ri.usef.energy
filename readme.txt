Building the project
- Required to have Java JDK 8 installed.
- Required to have maven 3 installed.
- Execute build.sh to do a full build in the right order.

Workspace settings
- Text file encoding: UTF-8 (Window -> Preferences -> General -> Workspace)
- New text file line delimiter: Unix (same dialog)
- Import formating file: usef-format-config.xml (Window -> Preferences ->
  Java -> Code Style -> Formatter)
	- spaces for indentation (4 spaces)
	- line length changed from 80 to 132
	- no new line after @param
	- enables never join already wrapped lines

Code templates
- Enable "Automatically add comments for new methods and types"
  (Window -> Preferences -> Java -> Code Style -> Code Templates)
- Import formating file: usef-codetemplates.xml (same dialog)
    - No @author any more
    - Copyright section at the top

Save actions
- Enable "Format source code" and "Organize imports" in Save actions
  dialog (Window -> Preferences -> Java -> Editor -> Save Actions)
