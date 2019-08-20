package lambda

import lambda.syntax.Name
import org.junit.Test

import org.junit.Assert.*

class EvalKtTest {

    @Test
    fun `should substitute var`(){
        stc("x", "y", "x", "y")
    }

    @Test
    fun `should not  substitute var`() {
        stc("k", "y", "x", "x")
    }

    @Test
    fun `should substitute in app`(){
        stc("x", "y", "x x", "y y")
        stc("x", "\\x.x", "x (y x)", "(\\x.x) (y \\x.x)")
    }

    @Test
    fun `should substitute in lambda`(){
        stc("x", "y", "\\k.x", "\\k.y")
    }

    @Test
    fun `should not substitute in lambda`(){
        stc("x", "y", "\\x.x", "\\x.x")
    }

    private fun stc(scrutinee: String, replacement: String, expr: String, expected: String){
        val replacement_ = Parser(Lexer(replacement)).parseExpression().value
        val scrutinee_ = Name(scrutinee)
        val expr_ = Parser(Lexer(expr)).parseExpression().value
        val expected_= Parser(Lexer(expected)).parseExpression().value
        assertEquals(EvalExpression.fromExpr(expected_), Eval().substitute(scrutinee_, EvalExpression.fromExpr(replacement_), EvalExpression.fromExpr(expr_)))
    }

    @Test
    fun `should return free vars`() {
        fvtc("x y z", listOf("x", "y", "z"))
        fvtc("\\x.x z", listOf("z"))
        fvtc("(\\x.x) x y z", listOf("x", "y", "z"))
    }

    private fun fvtc(input: String, expectedOutput: List<String>) {
        val parser = Parser(Lexer(input))
        assertEquals(EvalExpression.fromExpr(parser.parseExpression().value).freeVars(), expectedOutput.map(::Name).toSet())
    }
}