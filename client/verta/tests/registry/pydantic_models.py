from dataclasses import dataclass

from pydantic import BaseModel


@dataclass
class InnerInputClass(BaseModel):
    h_dict: dict
    i_list_str: list[str]

    def __init__(self, h, i):
        super().__init__(
            h_dict=h,
            i_list_str=i,
        )


@dataclass
class InputClass(BaseModel):
    a_int: int
    b_str: str
    c_float: float
    d_bool: bool
    e_list_int: list[int]
    f_dict: dict
    g_inner: InnerInputClass

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