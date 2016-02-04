from fabric.api import *
import os
import uuid
import hashlib
import time

code = os.environ.get('DJ_CODE', uuid.uuid4().hex)
print 'using code: {}'.format(code)

target = 'dj-assembly-0.0.1.jar'

program_jar = os.environ.get('PROGRAM_JAR', None)
main_cls = os.environ.get('MAIN_CLS', 'testcase.SimpleSerializationTest')
djit_cls = os.environ.get('DJIT_CLS', 'edu.berkeley.dj.jit.SimpleJIT')
extra_java_args = os.environ.get('EXTRA_JAVA_ARGS', '')

# allow running more then one, set this env variable to seperate between instances
fab_code = os.environ.get('FAB_CODE', 'dj_default')

def short_hash(h):
    return hashlib.md5(h).hexdigest()[:10]

def short_uid():
    return uuid.uuid4().hex[:6]

@task
@parallel
def host_type():
    print env.host
    run('uname -a')


@task
@parallel
def copy():
    # TODO: maybe use rsync here
    run('mkdir -p /tmp/{fab_code}/'.format(fab_code=fab_code))
    put('../target/scala-2.11/{}'.format(target), '/tmp/{fab_code}/'.format(fab_code=fab_code))
    # if program_jar is not None:
    #     put(program_jar, '/tmp/')


@parallel
def start_client():
    run("tmux new -d -s dj-session-{fab_code}-{hash} 'java -Xmx2g -Ddj.cluster_seed={hosts} -jar /tmp/{fab_code}/{target} -mode client -cluster_conn hazelcast -cluster_code {code} 2>&1 > /tmp/dj-log-{fab_code}-{short_code}-{hash}' > /dev/null".format(
        target=target,
        #id=env.hosts.index(env.host),
        hosts=env.dj_hosts,
        code=code,
        hash=short_uid(),
        short_code=code[:6],
        fab_code=fab_code,
    ))

def start_master():
    run("tmux new -d -s dj-session-master-{fab_code} 'java {extra_java_args} -Xmx2g -Ddj.cluster_seed={hosts} -jar /tmp/{fab_code}/{target} -mode master -cluster_conn hazelcast -cluster_code {code} -cp /tmp/{fab_code}/{program_jar} -maincls {main_cls} -djit {djit_cls} -debug_clazz_bytecode /tmp/djcls-{fab_code}/ 2>&1 > /tmp/dj-log-master-{fab_code}-{short_code}-{hash}' > /dev/null".format(
        target=target,
        code=code,
        hosts=env.dj_hosts,
        program_jar=program_jar.split('/')[-1] if program_jar is not None else target,
        main_cls=main_cls,
        djit_cls=djit_cls,
        hash=short_uid(),
        short_code=code[:6],
        fab_code=fab_code,
        extra_java_args=extra_java_args,
    ))

@task
@runs_once
def start_remote():
    env.dj_hosts = ','.join([h.split('@')[-1] for h in env.hosts])
    master_r = execute(start_master, hosts=[env.hosts[0]])
    time.sleep(7)
    client_r = execute(start_client, hosts=env.hosts[1:])


@task
@runs_once
def start():
    env.dj_hosts = ','.join([h.split('@')[-1] for h in env.hosts])
    client_r = execute(start_client, hosts=env.hosts)
    time.sleep(7)
    local("tmux new -d -s dj-session-master-{fab_code} 'java {extra_java_args} -Xmx2g -Ddj.cluster_seed={hosts} -jar ../target/scala-2.11/{target} -mode master -cluster_conn hazelcast -cluster_code {code} -cp ../target/scala-2.11/{target} -maincls {main_cls} -djit {djit_cls} -debug_clazz_bytecode /tmp/djcls/ 2>&1 >> /tmp/dj-log-master-{fab_code}' > /dev/null".format(
        target=target,
        code=code,
        hosts=env.dj_hosts,
        #program_jar=program_jar.split('/')[-1] if program_jar is not None else target,
        main_cls=main_cls,
        djit_cls=djit_cls,
        hash=short_uid(),
        short_code=code[:6],
        fab_code=fab_code,
        extra_java_args=extra_java_args,
    ))


def stop_master():
    run('tmux kill-session -t dj-session-master-{fab_code}'.format(fab_code=fab_code))

@task
@runs_once
def stop():
    local('tmux kill-session -t dj-session-master-{fab_code}'.format(fab_code=fab_code))

@task
@runs_once
def stop_remote():
    execute(stop_master, hosts=[env.hosts[0]])


@task
@parallel
def tail():
    run('tail -f `ls -1t /tmp/dj-log-{fab_code}* | head -n 1`'.format(fab_code=fab_code))


@task
def start_single():
    run(('java -ea -Ddj.classreload=false '

         '-jar /tmp/{target} -fjar /tmp/{target} -maincls testcase.Thrasher -debug_clazz_bytecode /tmp/djtcls/ -djit testcase.ThrasherJIT').format(
        target=target,
    ))
#           '-agentlib:jdwp=transport=dt_socket,server=n,address=10.7.0.5:5005,suspend=y '
