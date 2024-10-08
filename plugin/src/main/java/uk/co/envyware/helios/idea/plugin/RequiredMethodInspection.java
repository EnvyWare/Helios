package uk.co.envyware.helios.idea.plugin;

import com.intellij.codeInspection.*;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.tree.ChildRole;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import uk.co.envyware.helios.RequiredMethod;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class RequiredMethodInspection extends AbstractBaseJavaLocalInspectionTool {

    private static final String REQUIRED_METHOD = RequiredMethod.class.getCanonicalName();

    @Override
    public ProblemDescriptor @Nullable [] checkClass(@NotNull PsiClass aClass, @NotNull InspectionManager manager, boolean isOnTheFly) {
        List<ProblemDescriptor> problems = new ArrayList<>();

        for (var initializer : aClass.getInitializers()) {
            problems.addAll(this.findProblemsInCodeBlock(initializer.getBody(), manager, isOnTheFly));
        }

        return problems.toArray(new ProblemDescriptor[0]);
    }

    @Override
    public @Nullable ProblemDescriptor[] checkMethod(@NotNull PsiMethod method, @NotNull InspectionManager manager, boolean isOnTheFly) {
        var problems = new ArrayList<>(checkAnnotationContents(method, manager, isOnTheFly));
        var body = method.getBody();

        if (body == null) {
            return problems.toArray(new ProblemDescriptor[0]);
        }

        problems.addAll(this.findProblemsInCodeBlock(body, manager, isOnTheFly));

        return problems.toArray(new ProblemDescriptor[0]);
    }

    private List<ProblemDescriptor> findProblemsInCodeBlock(PsiCodeBlock codeBlock, InspectionManager manager, boolean isOnTheFly) {
        if (codeBlock == null) {
            return List.of();
        }

        List<ProblemDescriptor> problems = new ArrayList<>();

        for (var statement : codeBlock.getStatements()) {
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

            var requiredMethodValue = this.findRequiredMethodAnnotation(referencedMethod);

            if (requiredMethodValue == null) {
                continue;
            }

            for (var child : requiredMethodValue.getChildren()) {
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

        return problems;
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
        var requiredMethodValue = this.findRequiredMethodAnnotation(method);

        if (requiredMethodValue == null) {
            return List.of();
        }

        List<ProblemDescriptor> problems = new ArrayList<>();

        for (var child : requiredMethodValue.getChildren()) {
            if (!(child instanceof PsiLiteralExpression literal)) {
                continue;
            }

            var text = literal.getText().replace("\"", "");
            var targetMethod = method.getContainingClass().findMethodsByName(text, true);

            if (targetMethod.length == 0) {
                problems.add(manager.createProblemDescriptor(literal, "Cannot find method '" + text + "'", isOnTheFly, new LocalQuickFix[0], ProblemHighlightType.ERROR));
            }
        }

        return problems;
    }

    private PsiAnnotationMemberValue findRequiredMethodAnnotation(PsiMethod method) {
        if (method == null) {
            return null;
        }

        for (var annotation : method.getAnnotations()) {
            if (!annotation.getQualifiedName().equals(REQUIRED_METHOD)) {
                continue;
            }

            var value = annotation.findAttributeValue("value");

            if (value == null) {
                continue;
            }

            return value;
        }

        return null;
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

        if (method.getContainingClass().getName().equals(targetMethod.getContainingClass().getName()) && method.getName().equals(targetMethod.getName())) {
            return true;
        }

        var body = method.getBody();

        if (body == null) {
            return true;
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
