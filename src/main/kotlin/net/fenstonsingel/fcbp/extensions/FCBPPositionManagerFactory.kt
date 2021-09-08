package net.fenstonsingel.fcbp.extensions

import com.intellij.debugger.NoDataException
import com.intellij.debugger.PositionManager
import com.intellij.debugger.PositionManagerFactory
import com.intellij.debugger.SourcePosition
import com.intellij.debugger.engine.DebugProcess
import com.intellij.debugger.engine.PositionManagerEx
import com.intellij.debugger.engine.evaluation.EvaluationContext
import com.intellij.debugger.jdi.StackFrameProxyImpl
import com.intellij.debugger.requests.ClassPrepareRequestor
import com.intellij.openapi.fileTypes.FileType
import com.intellij.util.ThreeState
import com.sun.jdi.Location
import com.sun.jdi.ReferenceType
import com.sun.jdi.request.ClassPrepareRequest

class FCBPPositionManagerFactory : PositionManagerFactory() {

    override fun createPositionManager(p: DebugProcess): PositionManager = FCBPPositionManager

    object FCBPPositionManager : PositionManagerEx() {

        override fun evaluateCondition(
            context: EvaluationContext,
            frame: StackFrameProxyImpl,
            location: Location,
            expression: String
        ): ThreeState {
            // TODO figure out if the condition was already evaluated by virtue of being instrumented into code
            //      in other words, look up given info in the set of all instrumented breakpoints
            //      to see if it's one of them

            // the condition in question wasn't instrumented, let other evaluators decide whether it's true
            return ThreeState.UNSURE
        }

        /*
         * this is not a valid position manager, but merely a stub to be able to interrupt condition evaluations
         * therefore, all methods unrelated to that goal return a "I have no idea what to do with this" exception and
         * the list of accepted file types is empty
         */

        override fun getSourcePosition(l: Location?): SourcePosition =
            throw NoDataException.INSTANCE

        override fun getAllClasses(p: SourcePosition): MutableList<ReferenceType> =
            throw NoDataException.INSTANCE

        override fun locationsOfLine(t: ReferenceType, p: SourcePosition): MutableList<Location> =
            throw NoDataException.INSTANCE

        override fun createPrepareRequest(r: ClassPrepareRequestor, p: SourcePosition): ClassPrepareRequest =
            throw NoDataException.INSTANCE

        override fun getAcceptedFileTypes(): MutableSet<out FileType> = mutableSetOf()

    }

}
