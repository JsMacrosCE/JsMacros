package com.jsmacrosce.jsmacros.core.extensions;

import com.jsmacrosce.jsmacros.core.library.BaseLibrary;

import java.util.Set;

public interface LibraryExtension extends Extension {

    Set<Class<? extends BaseLibrary>> getLibraries();

}
