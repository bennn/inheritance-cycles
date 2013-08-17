Cycles
======
Analyzes the interitance hierarcy of some java code and identifies all cycles.

Install
-------
Clone the project. You should be all set.

Uninstall
---------
Delete the project. `rm -rf ~/*` or some less exciting variant.

Usage
-----
This is tricky. The command itself is simply. First find the root folder for your java project (say, `~/myJavaProject` and then run `./cycles ~/myJavaProject`. If the proper metadata exists inside the java project folder, you should see colorized output of all the cycles in the inheritance hierarchy of your java project.

Now about this metadata. The baby java compiler inside `AstVisualizer.java` needs a working `javac` command to function. Yes, this is a pain. It also needs a list of all java files to compile. 

Standard usage is `java -jar @argslist`,  where `argslist` is a list containing relative paths to all the java projects you want to run through the compiler.  So <b>requirement 1</b> is that you have a file called `argslist` at `myJavaProject/argslist` which contains a list of all the files to compile. You can generate this list with the command `find ~/myJavaProject -name "*.java" > ~/myJavaProject/argslist`, usually, but you may have to tweak it to remove some unwanted files. 

But your .java files probably won't compile without external libraries and other options. So <b>requirement 2</b> is that there exists a file `myJavaProject/compile.sh` which contains the call to javac that will compile all the files in `argslist`. The simplest `compile.sh` script is `javac @argslist`, but you will likely need to stick compiler options between the `javac` and the `argslist`. Sorry. I wish I could be more help here, but for the moment you're on your own. It'd really be something if there were some code that could infer the proper javac command from a `build.xml` or `pom.xml` file.

To summarize, make certain that `myJavaProject/argslist` and `myJavaProject/compile.sh` and contain a list of java files you want compiled and a working call to javac using these files, respectively. Have fun.

Licensing
---------
There are some nice parts of this project, so feel free to cut and paste whatever you find useful. Project released under the beer license. 

Ben Greenman, 2013-08-17

