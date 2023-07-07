from dataclasses import dataclass

from pydantic import BaseModel


@dataclass
class AnInnerClass(BaseModel):
    h_dict: dict
    i_list_str: list[str]

    def __init__(self, h, i):
        super().__init__(
            h_dict=h,
            i_list_str=i,
        )


@dataclass
class AClass(BaseModel):
    a_int: int
    b_str: str
    c_float: float
    d_bool: bool
    e_list_int: list[int]
    f_dict: dict
    g_inner: AnInnerClass

    def __init__(self, a, b, c, d, e, f, g):
        super().__init__(
            a_int=a,
            b_str=b,
            c_float=c,
            d_bool=d,
            e_list_int=e,
            f_dict=f,
            g_inner=g,
        )


@dataclass
class AnotherClass(BaseModel):
    j_bool: bool
    k_list_list_int: list[list[int]]
    l_str: str

    def __init__(self, j, k, l):
        super().__init__(
            j_bool=j,
            k_list_list_int=k,
            l_str=l,
        )
