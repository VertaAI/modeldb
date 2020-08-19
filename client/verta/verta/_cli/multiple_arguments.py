import click


class MultipleArguments:
    def __init__(self, argument, name):
        self.name = name
        self.argument = list(map(lambda s: s.split('='), argument))
        if self.argument and len(self.argument) > len(
                set(map(lambda pair: pair[0], self.argument))):
            raise click.BadParameter("cannot have duplicate {} keys".format(name))

    def for_each(self, action, get_keys, overwrite):
        if self.argument:
            argument_keys = set(get_keys())

            for pair in self.argument:
                if len(pair) != 2:
                    raise click.BadParameter("key and path for {}s must be separated by a '='".format(self.name))
                (key, _) = pair
                if key == "model":
                    raise click.BadParameter("the key \"model\" is reserved for model")

                if not overwrite and key in argument_keys:
                    raise click.BadParameter(
                        "key \"{}\" already exists; consider using --overwrite flag".format(key))

            for (key, path) in self.argument:
                action(key, path)
