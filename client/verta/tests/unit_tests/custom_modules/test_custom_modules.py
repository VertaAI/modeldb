# -*- coding: utf-8 -*-


def test_custom_modules(custom_module_factory):
    mod = custom_module_factory("bloo")
    print(f"{mod.__file__=}")
    print(f"{mod.__name__=}")

    mod = custom_module_factory("foo.bar.baz")
    print(f"{mod.__file__=}")
    print(f"{mod.__name__=}")
