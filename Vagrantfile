# -*- mode: ruby -*-
# vi: set ft=ruby :

ANSIBLE_VERSION = "2.3.1.0"
MOUNT_OPTIONS = if Vagrant::Util::Platform.linux? then
                  ['rw', 'vers=4', 'tcp', 'nolock']
                else
                  ['vers=3', 'udp']
                end

Vagrant.configure("2") do |config|

    # Ubuntu 14.04 LTS
    config.vm.box = "ubuntu/trusty64"

    # Ports to the services
    config.vm.network :forwarded_port, guest: 8777, host: 8777  # gt-chatta-demo
    config.vm.network :forwarded_port, guest: 8080, host: 8080  # spark master
    config.vm.network :forwarded_port, guest: 50095, host: 50095 # accumulo
    config.vm.network :forwarded_port, guest: 50070, host: 50070 # hdfs
    config.vm.network :private_network, ip: ENV.fetch("GT_TRANSIT_WEB_IP",  "10.10.10.10")
    # VM resource settings
    config.vm.provider :virtualbox do |vb|
        vb.memory = ENV.fetch("GT_TRANSIT_VM_MEMORY",  "4096")
        vb.cpus = 2
    end

    config.vm.synced_folder "~/.aws", "/home/vagrant/.aws"
    config.vm.synced_folder "./", "/home/vagrant/geotrellis-chatta-demo", type: "rsync",
       rsync__exclude: ["deployment/ansible/roles/azavea*/"],
       rsync__args: ["--verbose", "--archive", "-z"]

    # Provisioning
    # Ansible is installed automatically by Vagrant.
    config.vm.provision "ansible_local" do |ansible|
        ansible.install = true
        ansible.install_mode = :pip
        ansible.version = "#{ANSIBLE_VERSION}"
        ansible.playbook = "deployment/ansible/playbook.yml"
        ansible.galaxy_role_file = "deployment/ansible/roles.yml"
        ansible.galaxy_roles_path = "deployment/ansible/roles"
    end

    config.vm.provision "shell" do |s|
        s.path = 'deployment/vagrant/cd_shared_folder.sh'
        s.args = "/home/vagrant/geotrellis-chatta-demo"
    end

end
