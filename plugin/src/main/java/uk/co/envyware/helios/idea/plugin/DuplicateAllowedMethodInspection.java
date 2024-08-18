package uk.co.envyware.helios.idea.plugin;

import com.intellij.codeInspection.*;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiExpressionStatement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiMethodCallExpression;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import uk.co.envyware.helios.DuplicateAllowed;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DuplicateAllowedMethodInspection extends AbstractBaseJavaLocalInspectionTool {

    private static final String DUPLICATE_METHOD = DuplicateAllowed.class.getCanonicalName();

    @Override
    public @Nullable ProblemDescriptor[] checkMethod(@NotNull PsiMethod method, @NotNull InspectionManager manager, boolean isOnTheFly) {
        List<ProblemDescriptor> problems = new ArrayList<>();

        for (var statement : method.getBody().getStatements()) {
            if (!(statement instanceof PsiExpressionStatement expressionStatement)) {
                continue;
            }

            var expression = expressionStatement.getExpression();

            if(!(expression instanceof PsiMethodCallExpression methodCall) || methodCall.resolveMethod() == null) {
                continue;
            }

            Map<String, DuplicateMethod> nonDuplicateMethods = new HashMap<>();

            if (this.shouldRunInspection(methodCall)) {
                nonDuplicateMethods.computeIfAbsent(methodCall.resolveMethod().getName(), key -> new DuplicateMethod(methodCall)).incrementCounter();
            }

            var qualifier = methodCall.getMethodExpression().getQualifierExpression();

            while (qualifier instanceof PsiMethodCallExpression qualifierMethodCall && qualifierMethodCall.resolveMethod() != null) {
                qualifier = qualifierMethodCall.getMethodExpression().getQualifierExpression();

                if (this.shouldRunInspection(qualifierMethodCall)) {
                    var methodName = qualifierMethodCall.resolveMethod().getName();

                    nonDuplicateMethods.computeIfAbsent(methodName, key -> new DuplicateMethod(qualifierMethodCall)).incrementCounter();
                }
            }

            nonDuplicateMethods.forEach((methodName, count) -> {
                if (count.getCounter() > 1) {
                    problems.add(manager.createProblemDescriptor(count.getExpression(), "Duplicate method call '" + methodName + "'", true, new LocalQuickFix[0], ProblemHighlightType.GENERIC_ERROR_OR_WARNING));
                }
            });
        }

        return problems.toArray(new ProblemDescriptor[0]);
    }


    private boolean shouldRunInspection(PsiMethodCallExpression methodReference) {
        if (methodReference.resolveMethod() == null) {
            return false;
        }

        for (var annotation : methodReference.resolveMethod().getAnnotations()) {
            if (annotation.getQualifiedName().equals(DUPLICATE_METHOD)) {
                return false;
            }
        }

        return true;
    }

    private static class DuplicateMethod {

        private PsiExpression expression;
        private int counter = 0;

        public DuplicateMethod(PsiExpression expression) {
            this.expression = expression;
        }

        public PsiExpression getExpression() {
            return this.expression;
        }

        public int getCounter() {
            return this.counter;
        }

        public void incrementCounter() {
            this.counter++;
        }
    }
}
