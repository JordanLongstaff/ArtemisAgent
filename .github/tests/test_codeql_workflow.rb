# frozen_string_literal: true

require 'minitest/autorun'
require 'yaml'

class TestCodeQLWorkflow < Minitest::Test
  def setup
    @workflow_path = File.expand_path('../workflows/codeql.yml', __dir__)
    @workflow = YAML.load_file(@workflow_path)
  end

  def test_workflow_file_exists
    assert File.exist?(@workflow_path), "Workflow file should exist at #{@workflow_path}"
  end

  def test_workflow_has_correct_name
    assert_equal 'CodeQL', @workflow['name']
  end

  def test_workflow_triggers_on_push_to_main
    assert @workflow[true]['push'], 'Workflow should trigger on push'
    assert_includes @workflow[true]['push']['branches'], 'main'
  end

  def test_workflow_triggers_on_pull_request_to_main
    assert @workflow[true]['pull_request'], 'Workflow should trigger on pull_request'
    assert_includes @workflow[true]['pull_request']['branches'], 'main'
  end

  def test_workflow_triggers_on_schedule
    assert @workflow[true]['schedule'], 'Workflow should trigger on schedule'
    assert_equal 1, @workflow[true]['schedule'].length
    assert_equal '20 19 * * 0', @workflow[true]['schedule'][0]['cron']
  end

  def test_workflow_has_analyze_job
    assert @workflow['jobs']['analyze'], 'Workflow should have analyze job'
  end

  def test_analyze_job_configuration
    job = @workflow['jobs']['analyze']
    assert_equal 'ubuntu-latest', job['runs-on']
    assert_equal 120, job['timeout-minutes']
  end

  def test_analyze_job_has_required_permissions
    permissions = @workflow['jobs']['analyze']['permissions']
    assert permissions, 'Job should have permissions defined'
    assert_equal 'write', permissions['security-events']
    assert_equal 'read', permissions['packages']
    assert_equal 'read', permissions['actions']
    assert_equal 'read', permissions['contents']
  end

  def test_analyze_job_has_matrix_strategy
    strategy = @workflow['jobs']['analyze']['strategy']
    assert strategy, 'Job should have strategy'
    assert_equal false, strategy['fail-fast']
    assert strategy['matrix'], 'Strategy should have matrix'
  end

  def test_matrix_includes_all_languages
    matrix = @workflow['jobs']['analyze']['strategy']['matrix']
    includes = matrix['include']

    assert_equal 3, includes.length, 'Matrix should include 3 language configurations'

    languages = includes.map { |i| i['language'] }
    assert_includes languages, 'actions'
    assert_includes languages, 'java-kotlin'
    assert_includes languages, 'ruby'
  end

  def test_matrix_language_build_modes
    matrix = @workflow['jobs']['analyze']['strategy']['matrix']
    includes = matrix['include']

    actions_config = includes.find { |i| i['language'] == 'actions' }
    assert_equal 'none', actions_config['build-mode']

    java_config = includes.find { |i| i['language'] == 'java-kotlin' }
    assert_equal 'autobuild', java_config['build-mode']

    ruby_config = includes.find { |i| i['language'] == 'ruby' }
    assert_equal 'none', ruby_config['build-mode']
  end

  def test_analyze_job_has_concurrency_configuration
    concurrency = @workflow['jobs']['analyze']['concurrency']
    assert concurrency, 'Job should have concurrency configuration'
    assert concurrency['group'], 'Concurrency should have group'
    assert concurrency['cancel-in-progress'], 'Concurrency should cancel in progress'
  end

  def test_concurrency_group_includes_matrix_language
    group = @workflow['jobs']['analyze']['concurrency']['group']
    assert_match(/\$\{\{ github\.workflow \}\}/, group)
    assert_match(/\$\{\{ matrix\.language \}\}/, group)
    assert_match(/\$\{\{ github\.ref \}\}/, group)
  end

  def test_job_has_checkout_step
    steps = @workflow['jobs']['analyze']['steps']
    checkout_step = steps.find { |step| step['name'] == 'Checkout repository' }

    assert checkout_step, 'Should have checkout step'
    assert_match(/actions\/checkout@[a-f0-9]{40}/, checkout_step['uses'])
  end

  def test_job_has_conditional_secret_generation
    steps = @workflow['jobs']['analyze']['steps']
    secret_step = steps.find { |step| step['name'] == 'Generate secret files' }

    assert secret_step, 'Should have secret generation step'
    assert_equal "${{ matrix.language == 'java-kotlin' }}", secret_step['if']
    assert_match(/keystore\.properties/, secret_step['run'])
  end

  def test_job_has_conditional_jdk_setup
    steps = @workflow['jobs']['analyze']['steps']
    jdk_step = steps.find { |step| step['name'] == 'Set up JDK 21' }

    assert jdk_step, 'Should have JDK setup step'
    assert_equal "${{ matrix.language == 'java-kotlin' }}", jdk_step['if']
    assert_match(/actions\/setup-java@[a-f0-9]{40}/, jdk_step['uses'])
    assert_equal '.tool-versions', jdk_step['with']['java-version-file']
    assert_equal 'temurin', jdk_step['with']['distribution']
  end

  def test_job_has_codeql_initialization
    steps = @workflow['jobs']['analyze']['steps']
    init_step = steps.find { |step| step['name'] == 'Initialize CodeQL' }

    assert init_step, 'Should have CodeQL initialization step'
    assert_match(/github\/codeql-action\/init@[a-f0-9]{40}/, init_step['uses'])
    assert_equal '${{ matrix.language }}', init_step['with']['languages']
    assert_equal '${{ matrix.build-mode }}', init_step['with']['build-mode']
    assert_equal 'security-extended', init_step['with']['queries']
  end

  def test_job_has_manual_build_step
    steps = @workflow['jobs']['analyze']['steps']
    manual_build_step = steps.find { |step| step['if'] == "matrix.build-mode == 'manual'" }

    assert manual_build_step, 'Should have manual build step'
    assert_equal 'bash', manual_build_step['shell']
    assert_match(/exit 1/, manual_build_step['run'])
  end

  def test_job_has_codeql_analysis
    steps = @workflow['jobs']['analyze']['steps']
    analysis_step = steps.find { |step| step['name'] == 'Perform CodeQL Analysis' }

    assert analysis_step, 'Should have CodeQL analysis step'
    assert_match(/github\/codeql-action\/analyze@[a-f0-9]{40}/, analysis_step['uses'])
    assert_equal '/language:${{matrix.language}}', analysis_step['with']['category']
  end

  def test_all_actions_are_pinned_with_sha
    steps = @workflow['jobs']['analyze']['steps']
    steps.each do |step|
      next unless step['uses']

      assert_match(/@[a-f0-9]{40}/, step['uses'],
                   "Action '#{step['uses']}' should be pinned with SHA")
    end
  end

  def test_workflow_name_uses_matrix_variable
    job_name = @workflow['jobs']['analyze']['name']
    assert_equal 'Analyze (${{ matrix.language }})', job_name
  end

  def test_workflow_has_no_syntax_errors
    refute_nil @workflow, 'Workflow YAML should parse without errors'
  end

  def test_codeql_init_uses_security_extended_queries
    steps = @workflow['jobs']['analyze']['steps']
    init_step = steps.find { |step| step['name'] == 'Initialize CodeQL' }

    assert_equal 'security-extended', init_step['with']['queries']
  end

  def test_secret_generation_only_for_java_kotlin
    steps = @workflow['jobs']['analyze']['steps']
    secret_step = steps.find { |step| step['name'] == 'Generate secret files' }

    assert_includes secret_step['if'], 'java-kotlin',
                    'Secret generation should only run for java-kotlin'
  end

  def test_jdk_setup_only_for_java_kotlin
    steps = @workflow['jobs']['analyze']['steps']
    jdk_step = steps.find { |step| step['name'] == 'Set up JDK 21' }

    assert_includes jdk_step['if'], 'java-kotlin',
                    'JDK setup should only run for java-kotlin'
  end

  def test_codeql_category_uses_matrix_language
    steps = @workflow['jobs']['analyze']['steps']
    analysis_step = steps.find { |step| step['name'] == 'Perform CodeQL Analysis' }

    category = analysis_step['with']['category']
    assert_match(/matrix\.language/, category)
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

  def test_workflow_has_comments_for_guidance
    # Check that the workflow file content has helpful comments
    file_content = File.read(@workflow_path, encoding: 'UTF-8')
    assert_match(/NOTE/, file_content)
    assert_match(/CodeQL supports/, file_content)
  end

  def test_matrix_strategy_allows_partial_failures
    strategy = @workflow['jobs']['analyze']['strategy']
    assert_equal false, strategy['fail-fast'],
                 'Strategy should not fail fast to allow all languages to be analyzed'
  end
end