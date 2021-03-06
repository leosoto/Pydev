/* 
 * Copyright (C) 2006, 2007  Dennis Hunziker, Ueli Kistler 
 */

package ch.hsr.ukistler.astgraph;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.jgraph.graph.DefaultGraphCell;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.Tuple;
import org.python.pydev.parser.PyParser;
import org.python.pydev.parser.jython.SimpleNode;

/**
 * Starts the parser and its visitor (GraphVisitor)
 * 
 * @author Ueli Kistler
 * 
 */
public class ASTGraph {

    public Tuple<SimpleNode, Throwable> parseFile(String fileName) throws FileNotFoundException, IOException, Throwable {
        File pythonSource = new File(fileName);
        BufferedReader in = new BufferedReader(new FileReader(pythonSource));

        String line = "";
        StringBuilder source = new StringBuilder();
        while ((line = in.readLine()) != null) {
            source.append(line);
            source.append("\n");
        }

        IDocument doc = new Document(source.toString());
        Tuple<SimpleNode, Throwable> objects = PyParser.reparseDocument(new PyParser.ParserInfo(doc, false,
                IPythonNature.LATEST_GRAMMAR_VERSION));
        if (objects.o2 != null)
            throw objects.o2;
        return objects;
    }

    public DefaultGraphCell[] generateTree(SimpleNode node) throws IOException, Exception {
        GraphVisitor visitor = new GraphVisitor();
        node.accept(visitor);
        DefaultGraphCell[] cells = visitor.getCells();

        return cells;
    }

}
