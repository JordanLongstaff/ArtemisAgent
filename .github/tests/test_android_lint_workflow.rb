# frozen_string_literal: true

require 'minitest/autorun'
require 'yaml'

class TestAndroidLintWorkflow < Minitest::Test
  def setup
    @workflow_path = File.expand_path('../workflows/android-lint.yml', __dir__)
    @workflow = YAML.load_file(@workflow_path)
  end

  def test_workflow_file_exists
    assert File.exist?(@workflow_path), "Workflow file should exist at #{@workflow_path}"
  end

  def test_workflow_has_correct_name
    assert_equal 'Android Lint', @workflow['name']
  end

  def test_workflow_triggers_on_push_to_main
    # Note: YAML parser interprets 'on' key as boolean true
    assert @workflow[true]['push'], 'Workflow should trigger on push'
    assert_includes @workflow[true]['push']['branches'], 'main'
  end

  def test_workflow_triggers_on_pull_request_to_main
    # Note: YAML parser interprets 'on' key as boolean true
    assert @workflow[true]['pull_request'], 'Workflow should trigger on pull_request'
    assert_includes @workflow[true]['pull_request']['branches'], 'main'
  end

  def test_workflow_has_concurrency_configuration
    assert @workflow['concurrency'], 'Workflow should have concurrency configuration'
    assert @workflow['concurrency']['group'], 'Concurrency should have group'
    assert @workflow['concurrency']['cancel-in-progress'], 'Concurrency should cancel in progress'
  end

  def test_workflow_has_required_permissions
    permissions = @workflow['permissions']
    assert permissions, 'Workflow should have permissions defined'
    assert_equal 'write', permissions['pull-requests']
    assert_equal 'write', permissions['security-events']
  end

  def test_workflow_has_lint_job
    assert @workflow['jobs']['lint'], 'Workflow should have lint job'
  end

  def test_lint_job_runs_on_ubuntu
    lint_job = @workflow['jobs']['lint']
    assert_equal 'ubuntu-latest', lint_job['runs-on']
  end

  def test_lint_job_has_checkout_step
    steps = @workflow['jobs']['lint']['steps']
    checkout_step = steps.find { |step| step['name'] == 'Checkout repository' }

    assert checkout_step, 'Should have checkout step'
    assert checkout_step['uses'], 'Checkout step should use an action'
    assert_match(/actions\/checkout@[a-f0-9]{40}/, checkout_step['uses'])
  end

  def test_lint_job_has_secret_generation_step
    steps = @workflow['jobs']['lint']['steps']
    secret_step = steps.find { |step| step['name'] == 'Generate secret files' }

    assert secret_step, 'Should have secret generation step'
    assert secret_step['run'], 'Secret generation step should have run command'
    assert_match(/keystore\.properties/, secret_step['run'])
    assert_match(/google-services\.json/, secret_step['run'])
  end

  def test_lint_job_has_jdk_setup
    steps = @workflow['jobs']['lint']['steps']
    jdk_step = steps.find { |step| step['name'] == 'Set up JDK 21' }

    assert jdk_step, 'Should have JDK setup step'
    assert_match(/actions\/setup-java@[a-f0-9]{40}/, jdk_step['uses'])
    assert_equal '.tool-versions', jdk_step['with']['java-version-file']
    assert_equal 'temurin', jdk_step['with']['distribution']
  end

  def test_lint_job_has_gradle_setup
    steps = @workflow['jobs']['lint']['steps']
    gradle_step = steps.find { |step| step['name'] == 'Setup Gradle' }

    assert gradle_step, 'Should have Gradle setup step'
    assert_match(/gradle\/actions\/setup-gradle@[a-f0-9]{40}/, gradle_step['uses'])
  end

  def test_lint_job_has_android_lint_execution
    steps = @workflow['jobs']['lint']['steps']
    lint_step = steps.find { |step| step['name'] == 'Android Lint' }

    assert lint_step, 'Should have Android Lint execution step'
    assert_match(/burrunan\/gradle-cache-action@[a-f0-9]{40}/, lint_step['uses'])
    assert_equal 'lint', lint_step['with']['arguments']
  end

  def test_lint_job_has_report_step
    steps = @workflow['jobs']['lint']['steps']
    report_step = steps.find { |step| step['name'] == 'Report' }

    assert report_step, 'Should have Report step'
    assert_match(/hidakatsuya\/action-report-android-lint@[a-f0-9]{40}/, report_step['uses'])
    assert_equal 'app/**/reports/lint-results-debug.xml', report_step['with']['result-path']
    assert_equal false, report_step['with']['fail-on-warning']
  end

  def test_lint_job_has_sarif_upload_for_app
    steps = @workflow['jobs']['lint']['steps']
    sarif_step = steps.find { |step| step['name'] == 'Upload app SARIF report' }

    assert sarif_step, 'Should have SARIF upload step for app'
    assert_match(/github\/codeql-action\/upload-sarif@[a-f0-9]{40}/, sarif_step['uses'])
    assert_match(/lint-results-debug\.sarif/, sarif_step['with']['sarif_file'])
    assert_equal 'android-lint', sarif_step['with']['category']
  end

  def test_lint_job_has_sarif_upload_for_konsist
    steps = @workflow['jobs']['lint']['steps']
    sarif_step = steps.find { |step| step['name'] == 'Upload Konsist SARIF report' }

    assert sarif_step, 'Should have SARIF upload step for Konsist'
    assert_match(/github\/codeql-action\/upload-sarif@[a-f0-9]{40}/, sarif_step['uses'])
    assert_match(/konsist\/build\/reports\/lint-results-debug\.sarif/, sarif_step['with']['sarif_file'])
    assert_equal 'konsist', sarif_step['with']['category']
  end

  def test_all_actions_are_pinned_with_sha
    steps = @workflow['jobs']['lint']['steps']
    steps.each do |step|
      next unless step['uses']

      # Actions should be pinned with SHA (40 character hex)
      assert_match(/@[a-f0-9]{40}/, step['uses'],
                   "Action '#{step['uses']}' should be pinned with SHA")
    end
  end

  def test_workflow_steps_are_in_correct_order
    steps = @workflow['jobs']['lint']['steps']
    step_names = steps.map { |s| s['name'] }

    expected_order = [
      'Checkout repository',
      'Generate secret files',
      'Set up JDK 21',
      'Setup Gradle',
      'Android Lint',
      'Report',
      'Upload app SARIF report',
      'Upload Konsist SARIF report'
    ]

    assert_equal expected_order, step_names, 'Steps should be in the correct order'
  end

  def test_workflow_has_no_syntax_errors
    # If we got here and loaded YAML successfully, no syntax errors
    refute_nil @workflow, 'Workflow YAML should parse without errors'
  end

  def test_concurrency_group_uses_github_context
    group = @workflow['concurrency']['group']
    assert_match(/\$\{\{ github\.workflow \}\}/, group)
    assert_match(/\$\{\{ github\.ref \}\}/, group)
  end

  def test_secret_generation_uses_github_context
    steps = @workflow['jobs']['lint']['steps']
    secret_step = steps.find { |step| step['name'] == 'Generate secret files' }

    assert_match(/\$\{\{ secrets\.KEYSTORE_PROPERTIES \}\}/, secret_step['run'])
    assert_match(/\$\{\{ secrets\.GOOGLE_SERVICES \}\}/, secret_step['run'])
    assert_match(/\$\{\{ github\.workspace \}\}/, secret_step['run'])
  end

  def test_sarif_uploads_use_workspace_variable
    steps = @workflow['jobs']['lint']['steps']
    sarif_steps = steps.select { |s| s['name']&.include?('SARIF') }

    sarif_steps.each do |step|
      assert_match(/\$\{\{ github\.workspace \}\}/, step['with']['sarif_file'])
      assert_match(/\$\{\{ github\.workspace \}\}/, step['with']['checkout_path'])
    end
  end
end