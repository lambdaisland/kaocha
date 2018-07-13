# 9a6fa32592 (2018-06-02)

## Changed

- The configuration format has changed, you should now start with the `#kaocha
  {}` tagged reader literal in `tests.edn` to provide defaults. If you want more
  control then overwrite `tests.edn` with the output of `--print-config` and
  tweak.
