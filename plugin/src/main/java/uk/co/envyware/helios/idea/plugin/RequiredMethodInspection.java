package uk.co.envyware.helios.idea.plugin;

import com.intellij.codeInspection.*;
import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import uk.co.envyware.helios.RequiredMethod;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class RequiredMethodInspection extends AbstractBaseJavaLocalInspectionTool {

    private static final String REQUIRED_METHOD = RequiredMethod.class.getCanonicalName();

    @Override
    public @Nullable ProblemDescriptor[] checkMethod(@NotNull PsiMethod method, @NotNull InspectionManager manager, boolean isOnTheFly) {
        var problems = new ArrayList<>(checkAnnotationContents(method, manager, isOnTheFly));
        var body = method.getBody();

        if (body == null) {
            return problems.toArray(new ProblemDescriptor[0]);
        }

        for (var statement : body.getStatements()) {
            var methodCallExpression = checkStatement(statement, this::shouldRunInspection);

            if (methodCallExpression == null) {
                continue;
            }

            var referencedMethod = methodCallExpression.resolveMethod();

            if (referencedMethod == null) {
                continue;
            }

            var targetMethodClass = referencedMethod.getContainingClass();

            if (targetMethodClass == null) {
                continue;
            }

            for (var annotation : referencedMethod.getAnnotations()) {
                if (!annotation.getQualifiedName().equals(REQUIRED_METHOD)) {
                    continue;
                }

                var value = annotation.findAttributeValue("value");

                if (value == null) {
                    continue;
                }

                for (var child : value.getChildren()) {
                    if (!(child instanceof PsiLiteralExpression)) {
                        continue;
                    }

                    var targetMethod = child.getText().replace("\"", "");
                    var parsedTargetMethods = targetMethodClass.findMethodsByName(targetMethod, true);

                    for (var parsedTargetMethod : parsedTargetMethods) {
                        if (!canFindMethodCallInChain(methodCallExpression, parsedTargetMethod)) {
                            problems.add(manager.createProblemDescriptor(statement, "Missing required method call '" + parsedTargetMethod.getName() + "'", isOnTheFly, new LocalQuickFix[0], ProblemHighlightType.GENERIC_ERROR));
                        }
                    }
                }
            }
        }

        return problems.toArray(new ProblemDescriptor[0]);
    }

    private boolean shouldRunInspection(PsiMethod method) {
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

            if (value == null) {
                continue;
            }

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

    private static boolean canFindMethodCallInChain(PsiMethodCallExpression expression, PsiMethod targetMethod) {
        var reference = expression.resolveMethod();

        if (checkMethod(reference, targetMethod)) {
            return true;
        }

        var qualifier = expression.getMethodExpression().getQualifier();

        while (qualifier instanceof PsiMethodCallExpression methodCall) {
            qualifier = methodCall.getMethodExpression().getQualifierExpression();
            var resolvedMethod = methodCall.resolveMethod();

            if (checkMethod(resolvedMethod, targetMethod)) {
                return true;
            }
        }

        return false;
    }

    private static boolean checkMethod(PsiMethod method, PsiMethod targetMethod) {
        if (method == null) {
            return false;
        }

        if (method.getName().equals(targetMethod.getName())) {
            return true;
        }

        var body = method.getBody();

        if (body == null) {
            return false;
        }

        for (var statement : body.getStatements()) {
            if (checkStatement(statement, psiMethod -> checkMethod(psiMethod, targetMethod)) != null) {
                return true;
            }
        }

        return false;
    }

    private static PsiMethodCallExpression checkStatement(PsiElement statement, Predicate<PsiMethod> methodTest) {
        for (var child : statement.getChildren()) {
            var recursiveCheck = checkStatement(child, methodTest);

            if (recursiveCheck != null) {
                return recursiveCheck;
            }

            if (!(child instanceof PsiMethodCallExpression expressionStatement)) {
                continue;
            }

            if (methodTest.test(expressionStatement.resolveMethod())) {
                return expressionStatement;
            }
        }

        if (!(statement instanceof PsiMethodCallExpression expressionStatement)) {
            return null;
        }

        if (methodTest.test(expressionStatement.resolveMethod())) {
            return expressionStatement;
        }

        return null;
    }
}
