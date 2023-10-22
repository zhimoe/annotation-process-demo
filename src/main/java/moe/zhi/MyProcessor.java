package moe.zhi;


import java.io.IOException;
import java.io.PrintWriter;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import javax.tools.FileObject;

import static java.lang.StringTemplate.STR;

@SupportedAnnotationTypes({"moe.zhi.annotation.demo.Comparator"})
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class MyProcessor extends AbstractProcessor {

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (final Element element : roundEnv.getElementsAnnotatedWith(Comparator.class)) {
            if (element instanceof ExecutableElement m) {
                TypeElement className = (TypeElement) m.getEnclosingElement();
                Comparator annotation = m.getAnnotation(Comparator.class);
                if (annotation == null) {
                    continue;
                }
                TypeMirror returnType = m.getReturnType();
                if (!(returnType instanceof PrimitiveType)
                        || returnType.getKind() != TypeKind.INT) {
                    processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                            "@Comparator can only be applied to methods that return int");
                    continue;
                }
                // prepare for java file generation
                String theProcessedClassesName = className.getQualifiedName().toString();
                String comparatorClassName = annotation.value();
                String compareToMethodName = m.getSimpleName().toString();
                try {
                    writeComparatorFile(theProcessedClassesName, comparatorClassName, compareToMethodName);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } // end for
        return true; // claimed now,no need next processor
    }

    // be careful with spaces and ";" in content
    private void writeComparatorFile(String fullClassName, String comparatorClassName, String compareToMethodName)
            throws IOException {
        int lastIndexOfDot = fullClassName.lastIndexOf(".");
        String packageName = fullClassName.substring(0, lastIndexOfDot);
        FileObject sourceFile = processingEnv.getFiler().createSourceFile(packageName + "." + comparatorClassName);
        if (sourceFile == null) {
            System.out.println("create source file failed");
            throw new IOException("create source file failed:" + packageName + "." + comparatorClassName);
        }
        PrintWriter out = new PrintWriter(sourceFile.openWriter());

        String parametrizedType = fullClassName.substring(lastIndexOfDot + 1); //!! careful the index
        var packageLine = "";
        if (lastIndexOfDot > 0) {
            packageLine = STR."package \{ packageName };" ;
        }
        // 下面使用了Java 21的 STR 模板
        var content = STR."""
                \{ packageLine }

                public class \{ comparatorClassName }  implements java.util.Comparator<\{ parametrizedType }> {

                    public int compare( \{ parametrizedType } o1, \{ parametrizedType } o2 ){
                        return o1.\{ compareToMethodName }(o2);
                    }

                    public boolean equals(Object other) {
		                return this.getClass() == other.getClass();
	                }
                }
                """ ;
        out.println(content);
        out.close();
    }

}
