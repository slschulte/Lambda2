package lambda

sealed class EvalExpression {
    data class Literal(val lit: Lit) : EvalExpression()
    data class Var(val ident: Ident) : EvalExpression()
    data class Lambda(val binder: Ident, val body: EvalExpression) : EvalExpression()
    data class App(val func: EvalExpression, val arg: EvalExpression) : EvalExpression()
    data class Typed(val expr: EvalExpression, val type: Type): EvalExpression()

    companion object {
        fun fromExpr(expr: Expression): EvalExpression = when (expr) {
            is Expression.Literal -> Literal(expr.lit)
            is Expression.Var -> Var(expr.ident)
            is Expression.Lambda -> Lambda(expr.binder.value, fromExpr(expr.body.value))
            is Expression.App -> App(fromExpr(expr.func.value), fromExpr(expr.arg.value))
            is Expression.Typed -> Typed(fromExpr(expr.expr.value), expr.type.value)
        }
    }
}

class Eval {

    var freshSupply = 0

    // substitute(s, r, e) = [s -> r] e
    fun substitute(scrutinee: Ident, replacement: EvalExpression, expr: EvalExpression): EvalExpression {
        return when (expr) {
            is EvalExpression.Literal -> expr
            is EvalExpression.Var -> if (expr.ident == scrutinee) replacement else expr
            is EvalExpression.Lambda ->
                when {
                    expr.binder == scrutinee -> expr
                    replacement.freeVars().contains(expr.binder) -> {
                        val freshBinder = freshName(expr.binder)
                        val renamedBody = substitute(expr.binder, EvalExpression.Var(freshBinder), expr.body)
                        EvalExpression.Lambda(freshBinder, substitute(scrutinee, replacement, renamedBody))
                    }
                    else -> EvalExpression.Lambda(expr.binder, substitute(scrutinee, replacement, expr.body))
                }
            is EvalExpression.App ->
                EvalExpression.App(
                    substitute(scrutinee, replacement, expr.func),
                    substitute(scrutinee, replacement, expr.arg)
                )
            is EvalExpression.Typed -> EvalExpression.Typed(substitute(scrutinee, replacement, expr.expr), expr.type)
        }
    }

    private fun freshName(oldName: Ident): Ident {
        freshSupply++
        return Ident(freshSupply.toString() + oldName.ident)
    }

    fun eval(expr: EvalExpression): EvalExpression {
        return when (expr) {
            is EvalExpression.App -> when (val evaledFunc = eval(expr.func)) {
                is EvalExpression.Lambda -> {
                    eval(substitute(evaledFunc.binder, eval(expr.arg), evaledFunc.body))
                }
                else -> EvalExpression.App(evaledFunc, expr.arg)
            }
            else -> expr
        }
    }
}

fun EvalExpression.freeVars(): Set<Ident> {
    return when (this) {
        is EvalExpression.Literal -> emptySet()
        is EvalExpression.Var -> hashSetOf(ident)
        is EvalExpression.Lambda -> body.freeVars().filter { it != binder }.toSet()
        is EvalExpression.App -> func.freeVars().union(arg.freeVars())
        is EvalExpression.Typed -> expr.freeVars()
    }
}
