import LLVM.LLVMRegisterAllocator;
import ast.*;

import java.io.*;

public class Main {
    public static void main(String[] args) {
        try {
            var inputMethod = args[0];
            var action = args[1];
            var filename = args[args.length - 2];
            var outfilename = args[args.length - 1];

            Program prog;

            if (inputMethod.equals("parse")) {
                throw new UnsupportedOperationException("TODO - Ex. 4");
            } else if (inputMethod.equals("unmarshal")) {
                AstXMLSerializer xmlSerializer = new AstXMLSerializer();
                prog = xmlSerializer.deserialize(new File(filename));
            } else {
                throw new UnsupportedOperationException("unknown input method " + inputMethod);
            }
            var outFile = new PrintWriter(outfilename);

            try {
                if (action.equals("marshal")) {
                    AstXMLSerializer xmlSerializer = new AstXMLSerializer();
                    xmlSerializer.serialize(prog, outfilename);
                } else if (action.equals("print")) {
                    PrintProgram(prog, outFile);

                } else if (action.equals("semantic")) {
                    throw new UnsupportedOperationException("TODO - Ex. 3");

                } else if (action.equals("compile")) {
                    var llvmPrinter = new LLVMPrintVisitor();
                    llvmPrinter.visit(prog);
                    outFile.write(llvmPrinter.getString());

                } else if (action.equals("rename")) {
                    var type = args[2];
                    var originalName = args[3];
                    var originalLine = Integer.parseInt(args[4]);
                    var newName = args[5];
                    AstXMLSerializer xmlSerializer = new AstXMLSerializer();

                    boolean isMethod;
                    if (type.equals("var")) {
                        isMethod = false;
                    } else if (type.equals("method")) {
                        isMethod = true;
                    } else {
                        throw new IllegalArgumentException("unknown rename type " + type);
                    }

                    Visitor astChanger;

                    if (isMethod) {
                        var hierarchy = new ClassHierarchyForest(prog);
                        // We first rename the calls and only then the signatures
                        var predecessor = hierarchy.getHighestClassTreeByMethod(originalLine);
                        astChanger = new AstMethodCallsRenameVisitor(hierarchy, predecessor,
                                originalName, originalLine, newName);
                    } else {
                        astChanger = new AstFieldRenameVisitor(originalName, originalLine, newName);
                    }
                    astChanger.visit(prog);
                    xmlSerializer.serialize(prog, outfilename);

                } else {
                    throw new IllegalArgumentException("unknown command line action " + action);
                }
            } finally {
                outFile.flush();
                outFile.close();
            }

        } catch (FileNotFoundException e) {
            System.out.println("Error reading file: " + e);
            e.printStackTrace();
        } catch (Exception e) {
            System.out.println("General error: " + e);
            e.printStackTrace();
        }
    }

    public static void PrintProgram(Program node, PrintWriter outFile) {
        var astPrinter = new AstPrintVisitor();
        astPrinter.visit(node);
        outFile.write(astPrinter.getString());
    }
}
