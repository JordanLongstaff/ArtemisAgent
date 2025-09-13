# Make it more obvious that a PR is a work in progress and shouldn't be merged yet
warn("PR is marked with Work in Progress (WIP)") if github.pr_title.include? "WIP"

# Warn when there is a big PR
warn("PR affects more than 500 lines of code") if git.lines_of_code > 500

# Detekt
kotlin_detekt.skip_gradle_task = true
kotlin_detekt.report_file = "detekt.xml"
kotlin_detekt.filtering = true
kotlin_detekt.filtering_lines = true
kotlin_detekt.detekt(inline_mode: true)

# Android lint
android_lint.filtering = true
android_lint.filtering_lines = true

android_lint.report_file = "app/build/reports/lint-results-debug.xml"
android_lint.lint(inline_mode: true)

android_lint.report_file = "app/konsist/build/reports/lint-results-debug.xml"
android_lint.skip_gradle_task = true
android_lint.lint(inline_mode: true)

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
