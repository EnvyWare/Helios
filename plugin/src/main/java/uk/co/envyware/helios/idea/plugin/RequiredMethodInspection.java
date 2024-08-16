package uk.co.envyware.helios.idea.plugin;

import com.intellij.codeInspection.*;
import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import uk.co.envyware.helios.RequiredMethod;

import java.util.ArrayList;
import java.util.List;

public class RequiredMethodInspection extends AbstractBaseJavaLocalInspectionTool {

    private static final String REQUIRED_METHOD = RequiredMethod.class.getCanonicalName();

    @Override
    public @Nullable ProblemDescriptor[] checkMethod(@NotNull PsiMethod method, @NotNull InspectionManager manager, boolean isOnTheFly) {
        var problems = new ArrayList<>(checkAnnotationContents(method, manager, isOnTheFly));

        if (method.getBody() == null) {
            return problems.toArray(new ProblemDescriptor[0]);
        }

        for (var statement : method.getBody().getStatements()) {
            if (!(statement instanceof PsiExpressionStatement expressionStatement)) {
                continue;
            }

            var expression = expressionStatement.getExpression();

            if(!(expression instanceof PsiMethodCallExpression methodCall)) {
                continue;
            }

            var methodReference = methodCall.getMethodExpression();

            if (!this.shouldRunInspection(methodReference)) {
                continue;
            }

            var referencedMethod = (PsiMethod) methodReference.resolve();
            var targetMethodClass = referencedMethod.getContainingClass();

            for (var annotation : referencedMethod.getAnnotations()) {
                if (!annotation.getQualifiedName().equals(REQUIRED_METHOD)) {
                    continue;
                }

                var value = annotation.findAttributeValue("value");

                for (var child : value.getChildren()) {
                    if (!(child instanceof PsiLiteralExpression)) {
                        continue;
                    }

                    var targetMethod = child.getText().replace("\"", "");
                    var parsedTargetMethods = targetMethodClass.findMethodsByName(targetMethod, true);

                    for (var parsedTargetMethod : parsedTargetMethods) {
                        if (!canFindMethodCallInChain(methodCall.getMethodExpression(), parsedTargetMethod)) {
                            problems.add(manager.createProblemDescriptor(methodCall.getOriginalElement(), "Missing required method call '" + parsedTargetMethod.getName() + "'", isOnTheFly, new LocalQuickFix[0], ProblemHighlightType.GENERIC_ERROR));
                        }
                    }
                }
            }
        }

        return problems.toArray(new ProblemDescriptor[0]);
    }

    private boolean shouldRunInspection(PsiReferenceExpression reference) {
        var method = (PsiMethod) reference.resolve();

        if (method == null) {
            return false;
        }

        for (var annotation : method.getAnnotations()) {
            if (annotation.getQualifiedName().equals(REQUIRED_METHOD)) {
                return true;
            }
        }

        return false;
    }

    private List<ProblemDescriptor> checkAnnotationContents(PsiMethod method, InspectionManager manager, boolean isOnTheFly) {
        List<ProblemDescriptor> problems = new ArrayList<>();

        for (var annotation : method.getAnnotations()) {
            if (!annotation.getQualifiedName().equals(REQUIRED_METHOD)) {
                continue;
            }

            var value = annotation.findAttributeValue("value");

            for (var child : value.getChildren()) {
                if (!(child instanceof PsiLiteralExpression literal)) {
                    continue;
                }

                var text = literal.getText().replace("\"", "");
                var targetMethod = method.getContainingClass().findMethodsByName(text, true);

                if (targetMethod.length == 0) {
                    problems.add(manager.createProblemDescriptor(literal, "Cannot find method '" + text + "'", isOnTheFly, new LocalQuickFix[0], ProblemHighlightType.ERROR));
                }
            }
        }

        return problems;
    }

    private static boolean canFindMethodCallInChain(PsiReferenceExpression expression, PsiMethod targetMethod) {
        var qualifier = expression.getQualifierExpression();

        while (qualifier instanceof PsiMethodCallExpression methodCall) {
            qualifier = methodCall.getMethodExpression().getQualifierExpression();
            var resolvedMethod = methodCall.resolveMethod();

            if (resolvedMethod != null && resolvedMethod.getName().equals(targetMethod.getName())) {
                return true;
            }
        }

        return false;
    }
}
