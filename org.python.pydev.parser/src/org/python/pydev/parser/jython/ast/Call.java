// Autogenerated AST node
package org.python.pydev.parser.jython.ast;
import org.python.pydev.parser.jython.SimpleNode;
import java.io.DataOutputStream;
import java.io.IOException;

public class Call extends exprType {
    public exprType func;
    public exprType[] args;
    public keywordType[] keywords;
    public exprType starargs;
    public exprType kwargs;

    public Call(exprType func, exprType[] args, keywordType[] keywords,
    exprType starargs, exprType kwargs) {
        this.func = func;
        this.args = args;
        this.keywords = keywords;
        this.starargs = starargs;
        this.kwargs = kwargs;
    }

    public Call(exprType func, exprType[] args, keywordType[] keywords,
    exprType starargs, exprType kwargs, SimpleNode parent) {
        this(func, args, keywords, starargs, kwargs);
        this.beginLine = parent.beginLine;
        this.beginColumn = parent.beginColumn;
    }

    public String toString() {
        StringBuffer sb = new StringBuffer("Call[");
        sb.append("func=");
        sb.append(dumpThis(this.func));
        sb.append(", ");
        sb.append("args=");
        sb.append(dumpThis(this.args));
        sb.append(", ");
        sb.append("keywords=");
        sb.append(dumpThis(this.keywords));
        sb.append(", ");
        sb.append("starargs=");
        sb.append(dumpThis(this.starargs));
        sb.append(", ");
        sb.append("kwargs=");
        sb.append(dumpThis(this.kwargs));
        sb.append("]");
        return sb.toString();
    }

    public void pickle(DataOutputStream ostream) throws IOException {
        pickleThis(38, ostream);
        pickleThis(this.func, ostream);
        pickleThis(this.args, ostream);
        pickleThis(this.keywords, ostream);
        pickleThis(this.starargs, ostream);
        pickleThis(this.kwargs, ostream);
    }

    public Object accept(VisitorIF visitor) throws Exception {
        return visitor.visitCall(this);
    }

    public void traverse(VisitorIF visitor) throws Exception {
        if (func != null)
            func.accept(visitor);
        if (args != null) {
            for (int i = 0; i < args.length; i++) {
                if (args[i] != null)
                    args[i].accept(visitor);
            }
        }
        if (keywords != null) {
            for (int i = 0; i < keywords.length; i++) {
                if (keywords[i] != null)
                    keywords[i].accept(visitor);
            }
        }
        if (starargs != null)
            starargs.accept(visitor);
        if (kwargs != null)
            kwargs.accept(visitor);
    }

}