# frozen_string_literal: true

# Make it more obvious that a PR is a work in progress and shouldn't be merged yet
warn("PR is marked with Work in Progress (WIP)") if github.pr_title.include? "WIP"

# Warn when there is a big PR
warn("PR affects more than 500 lines of code") if git.lines_of_code > 500

# Detekt baselines
baseline_regex = %r{([A-Za-z]+/)*detekt-baseline(-[A-Za-z]+)*\.xml}
all_modified_files = (git.modified_files + git.added_files).uniq
baseline_files = all_modified_files.select { |f| baseline_regex.match?(f) }

baseline_files.each do |file|
    info = git.info_for_file(file)
    warn("Detekt warnings added to #{file}") if info[:insertions].positive?
end

# Custom logic for checking modified source files and corresponding tests
ian_src_regex = %r{IAN/([A-Za-z]+/)*src/(main|test|testFixtures)/}
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
    warn(':warning: Source files at #{path} were modified without also modifying tests')
end
