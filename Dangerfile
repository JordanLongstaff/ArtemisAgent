# frozen_string_literal: true

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

# Detekt baselines
baseline_regex = %r{([A-Za-z]+/)*detekt-baseline(-[A-Za-z]+)*\.xml}
all_modified_files = (git.modified_files + git.added_files).uniq
baseline_files = all_modified_files.select { |f| baseline_regex.match?(f) }

baseline_files.each do |file|
    info = git.info_for_file(file)
    warn("Detekt warnings added to #{github.html_link(file)}") if info[:insertions].positive?
end

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
all_modified_files = (git.modified_files + git.added_files).uniq
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

# Rubocop
rubocop.lint(
    inline_comment: true,
    group_inline_comments: true,
    report_danger: true,
    include_cop_names: true,
    only_report_new_offenses: true,
)

# LGTM
lgtm.check_lgtm(image_url: "https://firebasestorage.googleapis.com/v0/b/lgtmgen.appspot.com/o/images%2F99644be8-cf4b-4d41-a154-6be44b3be5eb.jpg?alt=media&token=062d9425-0ed7-4328-ad65-863509059853")
