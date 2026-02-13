# frozen_string_literal: true

require 'minitest/autorun'
require 'yaml'

class TestDangerWorkflow < Minitest::Test
  def setup
    @workflow_path = File.expand_path('../workflows/danger.yml', __dir__)
    @workflow = YAML.load_file(@workflow_path)
  end

  def test_workflow_file_exists
    assert File.exist?(@workflow_path), "Workflow file should exist at #{@workflow_path}"
  end

  def test_workflow_has_correct_name
    assert_equal 'Danger Review', @workflow['name']
  end

  def test_workflow_triggers_on_push_to_main
    assert @workflow[true]['push'], 'Workflow should trigger on push'
    assert_includes @workflow[true]['push']['branches'], 'main'
  end

  def test_workflow_triggers_on_pull_request_to_main
    assert @workflow[true]['pull_request'], 'Workflow should trigger on pull_request'
    assert_includes @workflow[true]['pull_request']['branches'], 'main'
  end

  def test_workflow_triggers_on_merge_group
    assert @workflow[true]['merge_group'], 'Workflow should trigger on merge_group'
    assert_includes @workflow[true]['merge_group']['types'], 'checks_requested'
  end

  def test_workflow_triggers_on_schedule
    assert @workflow[true]['schedule'], 'Workflow should trigger on schedule'
    assert_equal 1, @workflow[true]['schedule'].length
    assert_equal '15 1 * * 2', @workflow[true]['schedule'][0]['cron']
  end

  def test_workflow_triggers_on_workflow_dispatch
    assert @workflow[true].key?('workflow_dispatch'), 'Workflow should support manual trigger'
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
    assert_equal 'write', permissions['statuses']
  end

  def test_workflow_has_scan_job
    assert @workflow['jobs']['scan'], 'Workflow should have scan job'
  end

  def test_scan_job_runs_on_macos
    scan_job = @workflow['jobs']['scan']
    assert_equal 'macos-latest', scan_job['runs-on']
  end

  def test_scan_job_has_correct_name
    assert_equal 'Danger', @workflow['jobs']['scan']['name']
  end

  def test_scan_job_has_checkout_step
    steps = @workflow['jobs']['scan']['steps']
    checkout_step = steps.find { |step| step['name'] == 'Checkout repository' }

    assert checkout_step, 'Should have checkout step'
    assert_match(/actions\/checkout@[a-f0-9]{40}/, checkout_step['uses'])
  end

  def test_scan_job_has_conditional_secret_generation
    steps = @workflow['jobs']['scan']['steps']
    secret_step = steps.find { |step| step['name'] == 'Generate secret files' }

    assert secret_step, 'Should have secret generation step'
    assert_equal "github.event_name == 'pull_request'", secret_step['if']
    assert_match(/keystore\.properties/, secret_step['run'])
    assert_match(/google-services\.json/, secret_step['run'])
  end

  def test_scan_job_has_detekt_setup
    steps = @workflow['jobs']['scan']['steps']
    detekt_step = steps.find { |step| step['name'] == 'Setup Detekt' }

    assert detekt_step, 'Should have Detekt setup step'
    assert_match(/peter-murray\/setup-detekt@[a-f0-9]{40}/, detekt_step['uses'])
  end

  def test_scan_job_has_detekt_execution
    steps = @workflow['jobs']['scan']['steps']
    detekt_step = steps.find { |step| step['name'] == 'Run Detekt' }

    assert detekt_step, 'Should have Detekt execution step'
    assert_equal true, detekt_step['continue-on-error']
    assert_match(/detekt-cli/, detekt_step['run'])
    assert_match(/--parallel/, detekt_step['run'])
    assert_match(/detekt\.yml/, detekt_step['run'])
    assert_match(/detekt\.xml/, detekt_step['run'])
    assert_match(/detekt\.sarif\.json/, detekt_step['run'])
  end

  def test_scan_job_has_detekt_sarif_upload
    steps = @workflow['jobs']['scan']['steps']
    sarif_step = steps.find { |step| step['name'] == 'Upload Detekt SARIF report' }

    assert sarif_step, 'Should have Detekt SARIF upload step'
    assert_match(/github\/codeql-action\/upload-sarif@[a-f0-9]{40}/, sarif_step['uses'])
    assert_match(/detekt\.sarif\.json/, sarif_step['with']['sarif_file'])
    assert_match(/\$\{\{ github\.workspace \}\}/, sarif_step['with']['checkout_path'])
  end

  def test_scan_job_has_ruby_setup
    steps = @workflow['jobs']['scan']['steps']
    ruby_step = steps.find { |step| step['name'] == 'Setup Ruby' }

    assert ruby_step, 'Should have Ruby setup step'
    assert_match(/ruby\/setup-ruby@[a-f0-9]{40}/, ruby_step['uses'])
    assert_equal true, ruby_step['with']['bundler-cache']
  end

  def test_scan_job_has_rubocop_execution
    steps = @workflow['jobs']['scan']['steps']
    rubocop_step = steps.find { |step| step['name'] == 'Run Rubocop' }

    assert rubocop_step, 'Should have Rubocop execution step'
    assert_match(/bundle exec rubocop/, rubocop_step['run'])
    assert_match(/code_scanning/, rubocop_step['run'])
    assert_match(/CodeScanning::SarifFormatter/, rubocop_step['run'])
    assert_match(/rubocop\.sarif/, rubocop_step['run'])
  end

  def test_scan_job_has_rubocop_sarif_upload
    steps = @workflow['jobs']['scan']['steps']
    sarif_step = steps.find { |step| step['name'] == 'Upload Rubocop SARIF report' }

    assert sarif_step, 'Should have Rubocop SARIF upload step'
    assert_match(/github\/codeql-action\/upload-sarif@[a-f0-9]{40}/, sarif_step['uses'])
    assert_match(/rubocop\.sarif/, sarif_step['with']['sarif_file'])
  end

  def test_scan_job_has_danger_execution
    steps = @workflow['jobs']['scan']['steps']
    danger_step = steps.find { |step| step['name'] == 'Run Danger' }

    assert danger_step, 'Should have Danger execution step'
    assert_equal "github.event_name == 'pull_request'", danger_step['if']
    assert_match(/MeilCli\/danger-action@[a-f0-9]{40}/, danger_step['uses'])
  end

  def test_danger_step_configuration
    steps = @workflow['jobs']['scan']['steps']
    danger_step = steps.find { |step| step['name'] == 'Run Danger' }

    assert_equal 'Gemfile', danger_step['with']['plugins_file']
    assert_equal 'Dangerfile', danger_step['with']['danger_file']
    assert_equal 'danger-pr', danger_step['with']['danger_id']
  end

  def test_danger_step_uses_environment_variable
    steps = @workflow['jobs']['scan']['steps']
    danger_step = steps.find { |step| step['name'] == 'Run Danger' }

    assert danger_step['env'], 'Danger step should have environment variables'
    assert_equal '${{ secrets.DANGER_TOKEN }}', danger_step['env']['DANGER_GITHUB_API_TOKEN']
  end

  def test_all_actions_are_pinned_with_sha
    steps = @workflow['jobs']['scan']['steps']
    steps.each do |step|
      next unless step['uses']

      assert_match(/@[a-f0-9]{40}/, step['uses'],
                   "Action '#{step['uses']}' should be pinned with SHA")
    end
  end

  def test_workflow_steps_are_in_correct_order
    steps = @workflow['jobs']['scan']['steps']
    step_names = steps.map { |s| s['name'] }

    expected_order = [
      'Checkout repository',
      'Generate secret files',
      'Setup Detekt',
      'Run Detekt',
      'Upload Detekt SARIF report',
      'Setup Ruby',
      'Run Rubocop',
      'Upload Rubocop SARIF report',
      'Run Danger'
    ]

    assert_equal expected_order, step_names, 'Steps should be in the correct order'
  end

  def test_workflow_has_no_syntax_errors
    refute_nil @workflow, 'Workflow YAML should parse without errors'
  end

  def test_concurrency_group_uses_github_context
    group = @workflow['concurrency']['group']
    assert_match(/\$\{\{ github\.workflow \}\}/, group)
    assert_match(/\$\{\{ github\.ref \}\}/, group)
  end

  def test_secret_generation_only_for_pull_requests
    steps = @workflow['jobs']['scan']['steps']
    secret_step = steps.find { |step| step['name'] == 'Generate secret files' }

    assert_includes secret_step['if'], 'pull_request',
                    'Secret generation should only run for pull requests'
  end

  def test_danger_only_runs_for_pull_requests
    steps = @workflow['jobs']['scan']['steps']
    danger_step = steps.find { |step| step['name'] == 'Run Danger' }

    assert_includes danger_step['if'], 'pull_request',
                    'Danger should only run for pull requests'
  end

  def test_detekt_continues_on_error
    steps = @workflow['jobs']['scan']['steps']
    detekt_step = steps.find { |step| step['name'] == 'Run Detekt' }

    assert_equal true, detekt_step['continue-on-error'],
                 'Detekt should continue on error to allow other checks'
  end

  def test_rubocop_handles_exit_codes
    steps = @workflow['jobs']['scan']['steps']
    rubocop_step = steps.find { |step| step['name'] == 'Run Rubocop' }

    # Check that script handles exit code 2 (which indicates an error)
    assert_match(/\[\[ \$\? -ne 2 \]\]/, rubocop_step['run'])
  end

  def test_schedule_cron_is_valid_format
    cron = @workflow[true]['schedule'][0]['cron']
    parts = cron.split(' ')

    assert_equal 5, parts.length, 'Cron expression should have 5 parts'
    assert_match(/^\d+$/, parts[0]) # minute
    assert_match(/^\d+$/, parts[1]) # hour
    assert_equal '*', parts[2] # day of month
    assert_equal '*', parts[3] # month
    assert_match(/^\d+$/, parts[4]) # day of week
  end

  def test_detekt_outputs_multiple_formats
    steps = @workflow['jobs']['scan']['steps']
    detekt_step = steps.find { |step| step['name'] == 'Run Detekt' }

    # Should output both XML and SARIF
    assert_match(/-r xml:/, detekt_step['run'])
    assert_match(/-r sarif:/, detekt_step['run'])
  end

  def test_ruby_bundler_cache_enabled
    steps = @workflow['jobs']['scan']['steps']
    ruby_step = steps.find { |step| step['name'] == 'Setup Ruby' }

    assert_equal true, ruby_step['with']['bundler-cache'],
                 'Bundler cache should be enabled for faster builds'
  end

  def test_workflow_supports_multiple_trigger_types
    triggers = @workflow[true].keys
    expected_triggers = ['push', 'pull_request', 'merge_group', 'schedule', 'workflow_dispatch']

    expected_triggers.each do |trigger|
      assert_includes triggers, trigger, "Workflow should support #{trigger} trigger"
    end
  end
end