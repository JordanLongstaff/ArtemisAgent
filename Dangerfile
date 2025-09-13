# Make it more obvious that a PR is a work in progress and shouldn't be merged yet
warn("PR is marked with Work in Progress (WIP)") if github.pr_title.include? "WIP"

# Warn when there is a big PR
warn("PR affects more than 500 lines of code") if git.lines_of_code > 500

# Android lint
android_lint.report_file = "app/build/reports/lint-results-debug.xml"
android_lint.lint(inline_mode: true)

# Detekt
kotlin_detekt.gradle_task = "detekt"
kotlin_detekt.detekt

# Custom logic for checking modified source files and corresponding tests
ian_src_regex = %r{IAN/([A-Za-z]+/)*src/(main|test|testFixtures)/}
all_modified_files = (git.modified_files + git.created_files).uniq
ian_src_changes = all_modified_files
                  .select { |f| ian_src_regex.match?(f) }
                  .map { |f| File.dirname(f) }
                  .uniq

ian_src_test, ian_src_main = ian_src_changes.partition { |path| path.include?("src/test") }

main_only = ian_src_main.select do |main_path|
  test_regex_str = main_path.gsub("main", "test(Fixtures)?")
  test_regex = Regexp.new(test_regex_str)
  ian_src_test.none? { |test_path| test_regex.match?(test_path) }
end

main_only.each do |path|
  warn(":warning: Source files at #{path} were modified without also modifying tests")
end

# LGTM
lgtm.check_lgtm
