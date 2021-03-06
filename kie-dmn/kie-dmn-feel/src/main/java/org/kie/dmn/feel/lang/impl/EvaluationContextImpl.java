/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.kie.dmn.feel.lang.impl;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import org.kie.dmn.api.core.DMNRuntime;
import org.kie.dmn.api.feel.runtime.events.FEELEvent;
import org.kie.dmn.api.feel.runtime.events.FEELEventListener;
import org.kie.dmn.feel.lang.EvaluationContext;
import org.kie.dmn.feel.util.EvalHelper;

public class EvaluationContextImpl implements EvaluationContext {

    private final FEELEventListenersManager eventsManager;
    private ArrayDeque<ExecutionFrame> stack;
    private DMNRuntime dmnRuntime;
    private boolean performRuntimeTypeCheck = false;

    public EvaluationContextImpl(FEELEventListenersManager eventsManager) {
        this.eventsManager = eventsManager;
        this.stack = new ArrayDeque<>();
        // we create a rootFrame to hold all the built in functions
        push( RootExecutionFrame.INSTANCE );
        // and then create a global frame to be the starting frame
        // for function evaluation
        ExecutionFrameImpl global = new ExecutionFrameImpl( RootExecutionFrame.INSTANCE );
        push( global );
    }

    public EvaluationContextImpl(FEELEventListenersManager eventsManager, DMNRuntime dmnRuntime) {
        this(eventsManager);
        this.dmnRuntime = dmnRuntime;
    }

    public void push(ExecutionFrame obj) {
        stack.push( obj );
    }

    public ExecutionFrame pop() {
        return stack.pop();
    }

    public ExecutionFrame peek() {
        return stack.peek();
    }

    public Deque<ExecutionFrame> getStack() {
        return this.stack;
    }

    @Override
    public void enterFrame() {
        push( new ExecutionFrameImpl( peek() /*, symbols, scope*/ ) );
    }

    @Override
    public void exitFrame() {
        pop();
    }

    @Override
    public void setValue(String name, Object value) {
        peek().setValue( name, EvalHelper.coerceNumber( value ) );
    }
    
    public void setValues(Map<String, Object> values) {
        values.forEach(this::setValue);
    }

    @Override
    public Object getValue(String name) {
        return peek().getValue( name );
    }

    @Override
    public Object getValue(String[] name) {
        if (name.length == 1) {
          return getValue(name[0]);
        } else if (name.length > 1) {
            Map actualObject = (Map) peek().getValue(name[0]);
            // Each sublevel must be a context until the last one.
            for (int i = 1; i < name.length - 1; i++) {
                actualObject = (Map) actualObject.get(name[i]);
                if (actualObject == null) {
                    return null;
                }
            }
            return actualObject.get(name[name.length - 1]);
        } else {
            return null;
        }
    }

    @Override
    public boolean isDefined(String name) {
        return peek().isDefined( name );
    }

    @Override
    public boolean isDefined(String[] name) {
        if (name.length == 1) {
            return isDefined(name[0]);
        } else if (name.length > 1) {
            Map actualObject = (Map) peek().getValue(name[0]);
            // Each sublevel must be a context until the last one.
            for (int i = 1; i < name.length - 1; i++) {
                actualObject = (Map) actualObject.get(name[i]);
                if (actualObject == null) {
                    return false;
                }
            }
            return actualObject.get(name[name.length - 1]) != null;
        } else {
            return false;
        }
    }

    @Override
    public Map<String, Object> getAllValues() {
        Map<String, Object> values = new HashMap<>(  );
        for (ExecutionFrame frame : stack) {
            values.putAll( frame.getAllValues() );
        }
        return values;
    }

    @Override
    public void notifyEvt(Supplier<FEELEvent> event) {
        FEELEventListenersManager.notifyListeners(eventsManager, event);
    }

    @Override
    public Collection<FEELEventListener> getListeners() {
        return eventsManager.getListeners();
    }

    public DMNRuntime getDMNRuntime() {
        return dmnRuntime;
    }

    public void setPerformRuntimeTypeCheck(boolean performRuntimeTypeCheck) {
        this.performRuntimeTypeCheck = performRuntimeTypeCheck;
    }

    public boolean isPerformRuntimeTypeCheck() {
        return performRuntimeTypeCheck;
    }

    @Override
    public void setRootObject(Object v) {
        peek().setRootObject(v);
    }

    @Override
    public Object getRootObject() {
        return peek().getRootObject();
    }

}
