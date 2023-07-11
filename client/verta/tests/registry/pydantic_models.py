from typing import List

from pydantic import BaseModel


class AnInnerClass(BaseModel):
    h_dict: dict
    i_list_str: List[str]


class InputClass(BaseModel):
    a_int: int
    b_str: str
    c_float: float
    d_bool: bool
    e_list_int: List[int]
    f_dict: dict
    g_inner: AnInnerClass


class OutputClass(BaseModel):
    j_bool: bool
    k_list_list_int: List[List[int]]
    l_str: str
