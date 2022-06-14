package net.fenstonsingel.fcbp.core

import com.intellij.debugger.NoDataException
import com.intellij.debugger.SourcePosition
import com.intellij.debugger.engine.PositionManagerEx
import com.intellij.debugger.engine.evaluation.EvaluationContext
import com.intellij.debugger.jdi.StackFrameProxyImpl
import com.intellij.debugger.requests.ClassPrepareRequestor
import com.intellij.openapi.fileTypes.FileType
import com.intellij.util.ThreeState
import com.sun.jdi.Location
import com.sun.jdi.ReferenceType
import com.sun.jdi.request.ClassPrepareRequest

class FCBPPositionManager(private val session: FCBPSession) : PositionManagerEx() {

    override fun evaluateCondition(
        context: EvaluationContext,
        frame: StackFrameProxyImpl,
        location: Location,
        expression: String
    ): ThreeState = session.analyzeBreakpointConditionStatus(location)

    /*
     * this is not a valid position manager, but merely a stub to be able to interrupt condition evaluations
     * therefore, all methods unrelated to that goal return a "I have no idea what to do with this" exception and
     * the list of accepted file types is empty
     */

    override fun getSourcePosition(l: Location?): SourcePosition {
        throw NoDataException.INSTANCE
    }

    override fun getAllClasses(p: SourcePosition): MutableList<ReferenceType> {
        throw NoDataException.INSTANCE
    }

    override fun locationsOfLine(t: ReferenceType, p: SourcePosition): MutableList<Location> {
        throw NoDataException.INSTANCE
    }

    override fun createPrepareRequest(r: ClassPrepareRequestor, p: SourcePosition): ClassPrepareRequest {
        throw NoDataException.INSTANCE
    }

    override fun getAcceptedFileTypes(): MutableSet<out FileType> = mutableSetOf()

}
