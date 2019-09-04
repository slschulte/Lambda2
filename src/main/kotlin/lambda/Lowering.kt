package lambda

import lambda.syntax.DataConstructor
import lambda.syntax.Expression
import lambda.syntax.Name
import lambda.syntax.SourceFile

class Lowering() {

    private var table: Map<Name, List<DataConstructor>> = emptyMap()

    private fun lower(expr: Expression): RTExpression {
        return when (expr) {
            is Expression.Literal -> RTExpression.Literal(expr.lit)
            is Expression.Var -> RTExpression.Var(expr.name)
            is Expression.Lambda -> RTExpression.Lambda(
                expr.binder.value,
                lower(expr.body.value)
            )
            is Expression.App -> RTExpression.App(
                lower(expr.func.value),
                lower(expr.arg.value)
            )
            is Expression.Typed -> lower(expr.expr.value)
            is Expression.Let -> {
                // let x = 4 in add x 5
                // (\x. add x 5) 4

                RTExpression.App(
                    RTExpression.Lambda(expr.binder.value, lower(expr.body.value)),
                    lower(expr.expr.value)
                )
            }
            is Expression.If -> RTExpression.If(
                lower(expr.condition.value),
                lower(expr.thenBranch.value),
                lower(expr.elseBranch.value)
            )
            is Expression.Construction -> RTExpression.Pack(
                table.getValue(expr.type.value).indexOfFirst { it.name == expr.dtor.value } + 1, // TODO explode on non existing types or constructors
                expr.exprs.map { lower(it.value) }
            )
        }
    }

    fun lowerSourceFile(sf: SourceFile): List<Pair<Name, RTExpression>> {
        table = sf.typeDeclarations()
            .map { type -> type.name.value to type.dataConstructors }
            .toMap()

        return sf.valueDeclarations().map { it.name.value to lower(it.expr.value) }
    }
}

