import com.sun.source.util.JavacTask;

import java.io.IOException;

import java.util.*;
import java.io.File;
import java.io.FileNotFoundException;

import javax.lang.model.element.Element;
import javax.lang.model.type.TypeMirror;

import javax.tools.*;
import nu.xom.*;

public class AstVisualizer extends Object {

  public static void main(String[] args) {
    if (args.length < 1) {
      System.out.printf("Usage: ast-test [-out <outfile>] <file1> <file2> ... \n or ast-test @<file-with-list-of-filenames>\n");
    }
    else {
      javax.tools.JavaCompiler jc = ToolProvider.getSystemJavaCompiler();
      StandardJavaFileManager fm = jc.getStandardFileManager(new MyDiagnosticListener<JavaFileObject>(), null, null);
      Iterable<String> filenames = getFilenames(args);
      String fname, dirname;
      // Path to cwd, local name of cwd
      fname = System.getProperty("user.dir");
      dirname = fname.substring(fname.lastIndexOf('/') +1);
      // options for the compiler
      List<String> myOptions = getOptionsList(fname);
      // create XML document
      nu.xom.Element root, fileNode;
      root = new nu.xom.Element("files");
      root.addAttribute(new Attribute("project", dirname));
      // Reading in ALLLL the files at once
      Iterable<? extends JavaFileObject> fos = fm.getJavaFileObjectsFromStrings(filenames);
      // Analyze the project files
      JavaFileObject jfo = fos.iterator().next();
      fileNode = new nu.xom.Element("file");
      fileNode.addAttribute(new Attribute("name", jfo.getName()));
      JavacTask jct=(JavacTask) jc.getTask(null, fm, new MyDiagnosticListener<JavaFileObject>(), myOptions, null, fos);
      try {
        analyzeTarget(jct, fileNode);
      }
      catch (IOException e) {
        System.err.println("Failed to analyze compilation task, shutting down");
      }
      root.appendChild(fileNode);
      // print output
      try {
        Serializer s = new Serializer(System.out, "ISO-8859-1");
        s.setIndent(4);
        s.setMaxLength(64);
        s.write(new Document(root));
      } catch (IOException e) {
        System.err.println(e);
      }
    }
  }

  /**
   * Analyze the elements within a single compilation task
   * @param jct
   * @param fileNode
   * @throws IOException
   */
  private static void analyzeTarget(JavacTask jct, nu.xom.Element fileNode) throws IOException {
    try {
      nu.xom.Element elNode, nameNode, kindNode, superNode;
      String kind;
      List<? extends Object> supertypes;
      TypeMirror tm;
      for(Element el:jct.analyze()) {
        tm = el.asType();
        nameNode = getNameNode(tm);
        kindNode = new nu.xom.Element("kind");
        kind = el.getKind().toString();
        kindNode.appendChild(kind);
        if (kind == "PACKAGE") {
          // 2013-07-31: Other branch would throw illegal argument exception
          supertypes = new LinkedList<Object>();
        } else {
          supertypes = jct.getTypes().directSupertypes(tm);
        }
        superNode = getSupertypeNode(supertypes);
        elNode = new nu.xom.Element("element");
        elNode.appendChild(nameNode);
        elNode.appendChild(kindNode);
        elNode.appendChild(superNode);
        fileNode.appendChild(elNode);
      }
    } catch (NullPointerException npe) {
      nu.xom.Element ex = new nu.xom.Element("exception");
      ex.appendChild(npe.toString());
      fileNode.appendChild(ex);
    }
    return;
  }

  /** Return the list of filenames unchanged, or read in the list denoted
   * by the filename `args[0]`.
   *
   * @author blg59
   * @return list of filenames
   */
  private static Iterable<String> getFilenames(String[] args) {
    if (args[args.length-1].startsWith("@")) {
      String fname = args[0].substring(1);
      List<String> filenames = new LinkedList<String>();
      try {
        @SuppressWarnings("resource")
          Scanner s = new Scanner(new File(fname)).useDelimiter("\n");
        Iterator<String> i = (Iterator<String>) s;
        while (i.hasNext()) {
          filenames.add(i.next());
        }
        s.close();
      }
      catch (FileNotFoundException f) { 
        System.out.printf("Could not open file %s, shutting down", fname);
      }
      return filenames;
    } else {
      return Arrays.asList(args);
    }
  }

  /**
   * Analyze the name and type arguments of each class
   * @param tm
   * @return 
   */
  private static nu.xom.Element getNameNode(TypeMirror tm) {
    nu.xom.Element nameNode, typeArgNode, typeArgsNode;
    String typeStr, name, typeArgs;
    int splitIndex;
    nameNode = new nu.xom.Element("expanded-name");
    // typeStr is something like "package.ClassName<type1, type2, ...>"
    typeStr = tm.toString();
    if (typeStr.contains("<")) {
      splitIndex = typeStr.indexOf("<");
      name = typeStr.substring(0, splitIndex);
      typeArgs = typeStr.substring(splitIndex+1, typeStr.length()-1);
      typeArgsNode = new nu.xom.Element("name-type-args");
      for (String typeArg : typeArgs.split(" *, *")) {
        typeArgNode = new nu.xom.Element("type");
        typeArgNode.addAttribute(new Attribute("name", typeArg));
        typeArgsNode.appendChild(typeArgNode);
      }
      nameNode.appendChild(typeArgsNode);
    } else {
      name = typeStr;
    }
    nameNode.addAttribute(new Attribute("name", name));
    return nameNode;
  }

  /** 
   * Find the options passed to the (working) call to a 
   * compiler in the current directory. This is another annoying pre-requisite
   * @param fname
   * @return
   */
  private static List<String> getOptionsList(String fname) {
    ArrayList<String> options = new ArrayList<>();
    String word;
    Scanner s;
    try {
      s = new Scanner(new File(fname + "/compile.sh")).useDelimiter(" ");
      while (s.hasNext()) {
        word = s.next();
        if (!word.startsWith("java") && !word.startsWith("myjavac") && !word.startsWith("@")) {
          if (word.startsWith("\"") && word.endsWith("\"")) {
            // strip leading and trailing quotes
            word = word.substring(1, word.length()-1);
          }
          options.add(word);
        }
      }
      return options;
    } catch (FileNotFoundException e) {
      // No options. No big deal.
      return options;
    }
  }

  /**
   * Document all supertypes from a list, including their type arguments
   * @param xs
   * @return
   */
  private static nu.xom.Element getSupertypeNode(List<? extends Object> xs) {
    nu.xom.Element supertypesNode, superNode, typeArgsNode;
    int splitIndex;
    String name, supertypeStr, typeArgsStr;
    supertypesNode = new nu.xom.Element("supertypes");
    for (Object x : xs) {
      superNode = new nu.xom.Element("supertype");
      supertypeStr = x.toString();
      if (supertypeStr.contains("<")) {
        // Supertype has type arguments. 
        // First get the name out, then make subtree for type args.
        splitIndex = supertypeStr.indexOf("<");
        name = supertypeStr.substring(0, splitIndex);
        // Get everything inside the outermost <>
        typeArgsStr = supertypeStr.substring(splitIndex+1,supertypeStr.length()-1);
        // type arguments look like <type1,type2,...>
        // cut off the <> and iterate over arguments
        typeArgsNode = getTypeArgsNode(splitTypeArgs(typeArgsStr));
        superNode.appendChild(typeArgsNode);
      } else {
        // Supertype has no type arguments. Easy!
        name = supertypeStr;
      }
      superNode.addAttribute(new nu.xom.Attribute("name", name));
      supertypesNode.appendChild(superNode);
    }
    return supertypesNode;
  }

  private static nu.xom.Element getTypeArgsNode(String[] xs) {
    nu.xom.Element argsNode, typeNode;
    String typeStr, name, argsStr;
    int splitIndex;
    argsNode = new nu.xom.Element("type-args");
    for (int i=0; i<xs.length; i++) {
      typeNode = new nu.xom.Element("type");
      // Check if the type has any arguments
      typeStr = xs[i];
      if (typeStr.contains("<")) {
        // Recurse with arguments
        splitIndex = typeStr.indexOf("<");
        name = typeStr.substring(0, splitIndex);
        if (typeStr.length() == 0) {
          argsStr = "";
        } else {
          argsStr = typeStr.substring(splitIndex+1, typeStr.length()-1);
        }
        typeNode.appendChild(getTypeArgsNode(splitTypeArgs(argsStr)));
      } else {
        name = typeStr;
      }
      typeNode.addAttribute(new nu.xom.Attribute("name", name));
      argsNode.appendChild(typeNode);
    }
    return argsNode;
  }

  /**
   * Input will be something like "A,B,C" or "A<D,E>,B,C".
   * Want to split by comma, but only by outermost comma. Not sure why
   * this wasn't an issue earlier. (2013-07-31)
   * 
   * @param s
   * @return 
   */
  private static String[] splitTypeArgs(String s) {
    ArrayList<String> xs = new ArrayList<String>();
    int ignore = 0, numMatches = 1;
    String current = "";
    String[] matches;
    for (char c : s.toCharArray()) {
      if (c==',' && ignore==0) {
        numMatches++;
        xs.add(current);
        current = "";
      } else {
        current += c;
        if (c=='<') {
          ignore++;
        } else if (c=='>') {
          ignore--;
        }
      }
    }
    xs.add(current);
    matches = new String[numMatches];
    for (int i=0; i<numMatches; i++) {
      matches[i] = xs.get(i);
    }
    return matches;
  }

}
