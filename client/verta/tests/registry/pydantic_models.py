from pydantic import BaseModel


class AnInnerClass(BaseModel):
    h_dict: dict
    i_list_str: list[str]


class AClass(BaseModel):
    a_int: int
    b_str: str
    c_float: float
    d_bool: bool
    e_list_int: list[int]
    f_dict: dict
    g_inner: AnInnerClass


class AnotherClass(BaseModel):
    j_bool: bool
    k_list_list_int: list[list[int]]
    l_str: str
