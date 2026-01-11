package com.jsmacrosce.jsmacros.client.api.classes.worldscanner.filter.impl;

import com.jsmacrosce.jsmacros.client.api.classes.worldscanner.filter.ClassWrapperFilter;
import com.jsmacrosce.jsmacros.client.api.helper.world.BlockHelper;

import java.lang.reflect.Method;
import java.util.Map;

/**
 * @author Etheradon
 * @since 1.6.5
 */
public class BlockFilter extends ClassWrapperFilter<BlockHelper> {

    private static final Map<String, Method> METHOD_LOOKUP = getPublicNoParameterMethods(BlockHelper.class);

    public BlockFilter(String methodName, Object[] methodArgs, Object[] filterArgs) {
        super(methodName, METHOD_LOOKUP, methodArgs, filterArgs);
    }

}
