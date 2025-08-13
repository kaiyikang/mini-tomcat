package com.kaiyikang.minitomcat.engine;

import java.util.Collection;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.FilterRegistration.Dynamic;

public class FilterRegistrationImp implements Dynamic {
    final Filter filter;
}
