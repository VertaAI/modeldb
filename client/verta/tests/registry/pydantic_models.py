from dataclasses import dataclass

from pydantic import BaseModel


# TODO: Add json
@dataclass
class InnerInputClass(BaseModel):
    # h_dict: dict
    i_list_str: list[str]

    def __init__(self, i):
        super().__init__(
            i_list_str=i,
        )


@dataclass
class InputClass(BaseModel):
    a_int: int
    b_str: str
    c_float: float
    d_bool: bool
    e_list_int: list[int]
    # f_dict: dict
    g_inner: InnerInputClass

    def __init__(self, a, b, c, d, e, g):
        super().__init__(
            a_int=a,
            b_str=b,
            c_float=c,
            d_bool=d,
            e_list_int=e,
            g_inner=g,
        )
