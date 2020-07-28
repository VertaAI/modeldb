class AsciiTable:
    GAP = "   "
    def __init__(self, table):
        self.table = ""
        max_len = []
        for row in table:
            for i, elem in enumerate(row):
                if len(max_len) < len(row):
                    max_len.append(len(elem))
                else:
                    max_len[i] = max(max_len[i], len(elem))

        for row in table:
            for maxl, elem in zip(max_len, row):
                self.table += ("{:" + str(maxl) + "s}{}").format(elem, self.GAP)
            self.table += "\n"
